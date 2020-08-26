package au.com.dius.pact.provider.reporters

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.UrlPactSource
import au.com.dius.pact.core.pactbroker.VerificationNotice
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.VerificationResult
import com.github.ajalt.mordant.TermColors
import com.github.ajalt.mordant.TermColors.Level
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Pact verifier reporter that logs the results via SLF4J.
 */
@Suppress("TooManyFunctions")
class SLF4JReporter(
  var name: String,
  override var reportDir: File?,
  var displayFullDiff: Boolean
) : VerifierReporter {

  constructor(name: String, reportDir: File?) : this(name, reportDir, false)

  override val ext: String? = null
  override lateinit var verifier: IProviderVerifier

  private val log = LoggerFactory.getLogger(SLF4JReporter::class.java)

  override var reportFile: File
    get() = TODO("not implemented")
    set(_) {}

  override fun includesMetadata() {
    log.info("      includes message metadata")
  }

  override fun metadataComparisonOk() {
    log.info("      has matching metadata (OK)")
  }

  override fun metadataComparisonOk(key: String, value: Any?) {
    log.info("        \"$key\" with value \"$value\" (OK)")
  }

  override fun metadataComparisonFailed(key: String, value: Any?, comparison: Any) {
    log.info("        \"$key\" with value \"$value\" (FAILED)")
  }

  override fun initialise(provider: IProviderInfo) = Unit

  override fun finaliseReport() = Unit

  override fun reportVerificationForConsumer(consumer: IConsumerInfo, provider: IProviderInfo, tag: String?) {
    var out = "Verifying a pact between ${consumer.name}"
    if (!consumer.name.contains(provider.name)) {
      out += " and ${provider.name}"
    }
    if (tag != null) {
      out += " for tag $tag"
    }
    if (consumer.pending) {
      out += " [PENDING]"
    }
    log.info(out)
  }

  override fun verifyConsumerFromUrl(pactUrl: UrlPactSource, consumer: IConsumerInfo) {
    log.info("  [from ${pactUrl.description()}]")
  }

  override fun verifyConsumerFromFile(pactFile: PactSource, consumer: IConsumerInfo) {
    log.info("  [Using ${pactFile.description()}]")
  }

  override fun pactLoadFailureForConsumer(consumer: IConsumerInfo, message: String) = Unit

  override fun warnProviderHasNoConsumers(provider: IProviderInfo) {
    log.warn("         There are no consumers to verify for provider '${provider.name}'")
  }

  override fun warnPactFileHasNoInteractions(pact: Pact<Interaction>) {
    log.warn("         Pact file has no interactions")
  }

  override fun interactionDescription(interaction: Interaction) {
    log.info("  ${interaction.description}")
  }

  override fun stateForInteraction(
    state: String,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    isSetup: Boolean
  ) {
    log.info("  Given $state")
  }

  override fun warnStateChangeIgnored(state: String, provider: IProviderInfo, consumer: IConsumerInfo) {
    log.warn("         State Change ignored as there is no stateChange URL")
  }

  override fun stateChangeRequestFailedWithException(
    state: String,
    isSetup: Boolean,
    e: Exception,
    printStackTrace: Boolean
  ) {
    if (printStackTrace) {
      log.error("         State Change Request Failed - ${e.message}", e)
    } else {
      log.error("         State Change Request Failed - ${e.message}")
    }
  }

  override fun stateChangeRequestFailed(
    state: String,
    provider: IProviderInfo,
    isSetup: Boolean,
    httpStatus: String
  ) {
    log.info("         State Change Request Failed - $httpStatus")
  }

  override fun warnStateChangeIgnoredDueToInvalidUrl(
    state: String,
    provider: IProviderInfo,
    isSetup: Boolean,
    stateChangeHandler: Any
  ) {
    log.warn("         State Change ignored as there is no stateChange URL, received \"$stateChangeHandler\"")
  }

  override fun requestFailed(
    provider: IProviderInfo,
    interaction: Interaction,
    interactionMessage: String,
    e: Exception,
    printStackTrace: Boolean
  ) {
    if (printStackTrace) {
      log.error("      Request Failed - ${e.message}", e)
    } else {
      log.error("      Request Failed - ${e.message}")
    }
  }

  override fun returnsAResponseWhich() {
    log.info("    returns a response which")
  }

  override fun statusComparisonOk(status: Int) {
    log.info("      has status code $status (OK)")
  }

  override fun statusComparisonFailed(status: Int, comparison: Any) {
    log.info("      has status code $status (FAILED)")
  }

  override fun includesHeaders() {
    log.info("      includes headers")
  }

  override fun headerComparisonOk(key: String, value: List<String>) {
    val valuesStr = value.joinToString(", ")
    log.info("        \"$key\" with value \"$valuesStr\" (OK)")
  }

  override fun headerComparisonFailed(key: String, value: List<String>, comparison: Any) {
    val valuesStr = value.joinToString(", ")
    log.info("        \"$key\" with value \"$valuesStr\" (FAILED)")
  }

  override fun bodyComparisonOk() {
    log.info("      has a matching body (OK)")
  }

  override fun bodyComparisonFailed(comparison: Any) {
    log.info("      has a matching body (FAILED)")
  }

  override fun errorHasNoAnnotatedMethodsFoundForInteraction(interaction: Interaction) = Unit

  override fun verificationFailed(interaction: Interaction, e: Exception, printStackTrace: Boolean) {
    if (printStackTrace) {
      log.error("      Verification Failed - ${e.message}", e)
    } else {
      log.error("      Verification Failed - ${e.message}")
    }
  }

  override fun generatesAMessageWhich() {
    log.info("    generates a message which")
  }

  override fun displayFailures(failures: Map<String, Any>) {
    val result = StringBuilder()
    result.appendln("Failures:")
    failures.entries.forEachIndexed { i, err ->
      result.appendln("$i) ${err.key}")
      when {
        err.value is Throwable -> {
          result.appendln(prepareError(err.value as Throwable))
        }
        err.value is Map<*, *> &&
          (err.value as Map<*, *>).containsKey("comparison") &&
          (err.value as Map<*, *>)["comparison"] is Map<*, *>
        -> {
          result.appendln(prepareDiff(err.value as Map<String, Any>))
        }
        err.value is String -> {
          result.appendln("      ${err.value}")
        }
        err.value is Map<*, *> -> {
          for ((key, message) in err.value as Map<*, *>) {
            result.appendln("      $key -> $message")
          }
        }
        else -> {
          result.appendln(Json.toJson(err.value).serialise().prependIndent("      "))
        }
      }
    }
    log.info(result.toString())
  }

  override fun displayFailures(failures: List<VerificationResult.Failed>) {
    val nonPending = failures.filterNot { it.pending }
    val pending = failures.filter { it.pending }

    if (pending.isNotEmpty()) {
      log.error("Pending Failures:")
      pending.forEachIndexed { i, err -> displayFailure(i, err) }
    }

    if (nonPending.isNotEmpty()) {
      log.error("Failures:")
      nonPending.forEachIndexed { i, err -> displayFailure(i, err) }
    }
  }

  private fun displayFailure(i: Int, err: VerificationResult.Failed) {
    val t = TermColors(Level.NONE)
    log.error("${i + 1}) ${err.verificationDescription}\n")
    err.failures.forEachIndexed { index, failure ->
      val message = "    ${i + 1}.${index + 1}) ${failure.formatForDisplay(t)}"

      if (failure.hasException() && verifier.projectHasProperty.apply("pact.showStacktrace")) {
        log.error(message, failure.getException())
      } else {
        log.error(message)
      }
    }
  }

  override fun reportVerificationNoticesForConsumer(
    consumer: IConsumerInfo,
    provider: IProviderInfo,
    notices: List<VerificationNotice>
  ) {
    val result = StringBuilder()
    result.appendln("  Notices:")
    notices.forEachIndexed { i, notice -> result.appendln("    ${i + 1}) ${notice.text}") }

    log.info(result.toString())
  }

  override fun warnPublishResultsSkippedBecauseFiltered() {
    log.warn("NOTE: Skipping publishing of verification results as the interactions have been filtered")
  }

  override fun warnPublishResultsSkippedBecauseDisabled(envVar: String) {
    log.warn("NOTE: Skipping publishing of verification results as it has been disabled ($envVar is not 'true')")
  }

  private fun prepareDiff(diff: Map<String, Any>): String {
    val result = StringBuilder()

    val comparison = diff["comparison"] as Map<String, List<Map<String, Any>>>
    for ((key, messageAndDiff) in comparison) {
      for (mismatch in messageAndDiff) {
        result.appendln("      $key -> ${mismatch["mismatch"]}")

        val mismatchDiff = if (mismatch["diff"] is List<*>) {
          mismatch["diff"] as List<String>
        } else {
          listOf(mismatch["diff"].toString())
        }

        if (mismatchDiff.all { it.isEmpty() }) {
          continue
        }

        result.appendln("        Diff:")
        mismatchDiff
          .asSequence()
          .filter { it.isNotEmpty() }
          .forEach { result.appendln("        $it") }
      }
    }

    if (displayFullDiff) {
      result.appendln("      Full Diff:")
      for (delta in diff["diff"] as List<String>) {
        result.appendln("      $delta")
      }
    }

    return result.toString()
  }

  private fun prepareError(err: Throwable): String {
    return "      ${err.javaClass.name}: ${err.message}"
  }
}
