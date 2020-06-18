package au.com.dius.pact.provider

import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.Mismatch
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.pactbroker.TestResult
import au.com.dius.pact.core.support.isNotEmpty
import com.github.ajalt.mordant.TermColors
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getError

private fun padLines(str: String, indent: Int): String {
  val pad = " ".repeat(indent)
  return str.split('\n').joinToString("\n") { pad + it }
}

sealed class VerificationFailureType {
  abstract fun description(): String
  abstract fun formatForDisplay(t: TermColors): String
  abstract fun hasException(): Boolean
  abstract fun getException(): Throwable?

  data class MismatchFailure(
    val mismatch: Mismatch,
    val interaction: Interaction? = null,
    val pact: Pact<Interaction>? = null
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

  data class ExceptionFailure(val e: Throwable) : VerificationFailureType() {
    override fun description() = e.message ?: e.javaClass.name
    override fun formatForDisplay(t: TermColors): String {
      return if (e.message.isNotEmpty()) {
        padLines(e.message!!, 6)
      } else {
        "      ${e.javaClass.name}"
      }
    }

    override fun hasException() = true
    override fun getException() = e
  }

  data class StateChangeFailure(val result: StateChangeResult) : VerificationFailureType() {
    override fun description() = formatForDisplay(TermColors())
    override fun formatForDisplay(t: TermColors): String {
      val e = result.stateChangeResult.getError()
      return "State change callback failed with an exception - " + e?.message.toString()
    }

    override fun hasException() = result.stateChangeResult is Err
    override fun getException() = result.stateChangeResult.getError()
  }
}

/**
 * Result of verifying an interaction
 */
sealed class VerificationResult {
  /**
   * Result was successful
   */
  object Ok : VerificationResult() {
    override fun merge(result: VerificationResult) = when (result) {
      is Ok -> this
      is Failed -> result
    }

    override fun toTestResult() = TestResult.Ok
  }

  /**
   * Result failed
   */
  data class Failed(
    @Deprecated("use failures instead")
    var results: List<Map<String, Any?>> = emptyList(),
    val description: String = "",
    val verificationDescription: String = "",
    val failures: List<VerificationFailureType> = emptyList(),
    val pending: Boolean = false,
    val interactionId: String? = null
  ) : VerificationResult() {
    override fun merge(result: VerificationResult) = when (result) {
      is Ok -> this
      is Failed -> Failed(results + result.results, when {
        description.isNotEmpty() && result.description.isNotEmpty() && description != result.description ->
          "$description, ${result.description}"
        description.isNotEmpty() -> description
        else -> result.description
      }, verificationDescription, failures + result.failures, pending && result.pending,
        interactionId ?: result.interactionId)
    }

    override fun toTestResult() =
      TestResult.Failed(results.map { it + ("interactionId" to interactionId) }, description)
  }

  /**
   * Merge this result with the other one, creating a new result
   */
  abstract fun merge(result: VerificationResult): VerificationResult

  /**
   * Convert to a test result
   */
  abstract fun toTestResult(): TestResult
}
