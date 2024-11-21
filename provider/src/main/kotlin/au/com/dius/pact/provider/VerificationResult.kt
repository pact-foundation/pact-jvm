package au.com.dius.pact.provider

import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.matchers.MetadataMismatch
import au.com.dius.pact.core.matchers.Mismatch
import au.com.dius.pact.core.matchers.QueryMismatch
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.pactbroker.TestResult
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.isNotEmpty
import com.github.ajalt.mordant.TermColors

private fun padLines(str: String, indent: Int): String {
  val pad = " ".repeat(indent)
  val lines = str.split('\n')
  return lines.mapIndexed { i, line ->
    if (i == 0)
      line
    else
      pad + line
  }.joinToString("\n")
}

sealed class VerificationFailureType {
  abstract fun description(): String
  abstract fun formatForDisplay(t: TermColors): String
  abstract fun hasException(): Boolean
  abstract fun getException(): Throwable?

  data class MismatchFailure(
    val mismatch: Mismatch,
    val interaction: Interaction? = null,
    val pact: Pact? = null
  ) : VerificationFailureType() {
    override fun description() = formatForDisplay(TermColors())
    override fun formatForDisplay(t: TermColors): String {
      return when (mismatch) {
        is BodyMismatch -> {
          var description = "${mismatch.type()}: ${t.bold(mismatch.path)} ${mismatch.description(t)}"

          if (mismatch.diff.isNotEmpty()) {
            description += "\n\n" + formatDiff(t, mismatch.diff!!) + "\n"
          }

          description
        }
        else -> mismatch.type() + ": " + mismatch.description(t)
      }
    }

    override fun hasException() = false
    override fun getException() = null

    private fun formatDiff(t: TermColors, diff: String): String {
      val pad = " ".repeat(8)
      return diff.split('\n').joinToString("\n") {
        pad + when {
          it.startsWith('-') -> t.red(it)
          it.startsWith('+') -> t.green(it)
          else -> it
        }
      }
    }
  }

  data class ExceptionFailure(
    val description: String,
    val e: Throwable,
    val interaction: Interaction? = null
  ) : VerificationFailureType() {
    override fun description() = e.message ?: e.javaClass.name
    override fun formatForDisplay(t: TermColors): String {
      return if (e.message.isNotEmpty()) {
        padLines(e.message!!, 6)
      } else {
        padLines(e.toString(), 6)
      }
    }

    override fun hasException() = true
    override fun getException() = e
  }

  data class StateChangeFailure(
    val description: String,
    val result: StateChangeResult,
    val interaction: Interaction? = null,
  ) : VerificationFailureType() {
    override fun description() = formatForDisplay(TermColors())
    override fun formatForDisplay(t: TermColors): String {
      val e = result.stateChangeResult.errorValue()
      return "State change callback failed with an exception - " + e?.message.toString()
    }

    override fun hasException() = result.stateChangeResult is Result.Err
    override fun getException() = result.stateChangeResult.errorValue()
  }

  data class PublishResultsFailure(val cause: List<String>) : VerificationFailureType() {
    override fun description() = formatForDisplay(TermColors())
    override fun formatForDisplay(t: TermColors): String {
      return "Publishing verification results failed - \n" + cause.joinToString("\n") { "             $it" }
    }

    override fun hasException() = false
    override fun getException() = null
  }

  data class InvalidInteractionFailure(val cause: String) : VerificationFailureType() {
    override fun description() = formatForDisplay(TermColors())
    override fun formatForDisplay(t: TermColors) = cause

    override fun hasException() = false
    override fun getException() = null
  }
}

typealias VerificationFailures = Map<String, List<VerificationFailureType>>

/**
 * Result of verifying an interaction
 */
sealed class VerificationResult {
  /**
   * Result was successful
   */
  data class Ok @JvmOverloads constructor(
    val interactionIds: Set<String> = emptySet(),
    val output: List<String> = emptyList()
  ) : VerificationResult() {

    constructor(
      interactionId: String?,
      output: List<String>
    ) : this(if (interactionId.isNullOrEmpty()) emptySet() else setOf(interactionId), output)

    override fun merge(result: VerificationResult) = when (result) {
      is Ok -> this.copy(interactionIds = interactionIds + result.interactionIds, output = output + result.output)
      is Failed -> result.merge(this)
    }

    override fun toTestResult() = TestResult.Ok(interactionIds)
  }

  /**
   * Result failed
   */
  data class Failed @JvmOverloads constructor(
    val description: String = "",
    val verificationDescription: String = "",
    val failures: VerificationFailures = mapOf(),
    val pending: Boolean = false,
    @Deprecated("use failures instead")
    var results: List<Map<String, Any?>> = emptyList(),
    val output: List<String> = emptyList()
  ) : VerificationResult() {
    override fun merge(result: VerificationResult) = when (result) {
      is Ok -> this.copy(failures = failures + result.interactionIds
        .associateWith {
          (if (failures.containsKey(it)) failures[it] else emptyList<VerificationFailureType>())!!
        }, output = output + result.output)
      is Failed -> Failed(when {
        description.isNotEmpty() && result.description.isNotEmpty() && description != result.description ->
          "$description, ${result.description}"
        description.isNotEmpty() -> description
        else -> result.description
      }, verificationDescription, mergeFailures(failures, result.failures),
        pending && result.pending, output = output + result.output)
    }

    private fun mergeFailures(failures: VerificationFailures, other: VerificationFailures): VerificationFailures {
      return (failures.entries + other.entries).groupBy { it.key }
        .mapValues { entry -> entry.value.flatMap { it.value } }
    }

    override fun toTestResult(): TestResult {
      val failures = failures.flatMap { entry ->
        if (entry.value.isNotEmpty()) {
          entry.value.map { failure ->
            val errorMap = when (failure) {
              is VerificationFailureType.ExceptionFailure -> {
                val list = mutableListOf(
                  "exception" to failure.getException(),
                  "description" to failure.description
                )
                if (failure.interaction != null) {
                  list.add("interactionDescription" to failure.interaction.description)
                }
                list
              }
              is VerificationFailureType.StateChangeFailure -> {
                val list = mutableListOf(
                  "exception" to failure.getException(),
                  "description" to failure.description
                )
                if (failure.interaction != null) {
                  list.add("interactionDescription" to failure.interaction.description)
                }
                list
              }
              is VerificationFailureType.MismatchFailure -> {
                val list = mutableListOf<Pair<String, String?>>(
                  "attribute" to failure.mismatch.type(),
                  "description" to failure.mismatch.description()
                )
                when (val mismatch = failure.mismatch) {
                  is BodyMismatch -> {
                    list.add("identifier" to mismatch.path)
                    list.add("description" to mismatch.mismatch)
                    list.add("diff" to mismatch.diff)
                  }
                  is HeaderMismatch -> {
                    list.add("identifier" to mismatch.headerKey)
                    list.add("description" to mismatch.mismatch)
                  }
                  is QueryMismatch -> {
                    list.add("identifier" to mismatch.queryParameter)
                    list.add("description" to mismatch.mismatch)
                  }
                  is MetadataMismatch -> {
                    list.add("identifier" to mismatch.key)
                    list.add("description" to mismatch.mismatch)
                  }
                  else -> {}
                }
                if (failure.interaction != null) {
                  list.add("interactionDescription" to failure.interaction.description)
                }
                list
              }
              is VerificationFailureType.PublishResultsFailure -> listOf(
                "description" to failure.description()
              )
              is VerificationFailureType.InvalidInteractionFailure -> listOf("description" to failure.description())
            }
            (listOf("interactionId" to entry.key) + errorMap).toMap()
          }
        } else {
          listOf(mapOf("interactionId" to entry.key))
        }
      }
      return TestResult.Failed(failures, description)
    }
  }

  /**
   * Merge this result with the other one, creating a new result
   */
  abstract fun merge(result: VerificationResult): VerificationResult

  /**
   * Convert to a test result
   */
  abstract fun toTestResult(): TestResult

  /**
   * Return any output for the result
   */
  fun getResultOutput(): List<String> {
    return when (this) {
      is Failed -> this.output
      is Ok -> this.output
    }
  }
}
