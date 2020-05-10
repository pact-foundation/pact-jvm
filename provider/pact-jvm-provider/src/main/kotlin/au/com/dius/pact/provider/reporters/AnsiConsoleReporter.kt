package au.com.dius.pact.provider.reporters

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.UrlPactSource
import au.com.dius.pact.core.pactbroker.VerificationNotice
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.VerificationResult
import com.github.ajalt.mordant.TermColors
import java.io.File

/**
 * Pact verifier reporter that displays the results of the verification to the console using ASCII escapes
 */
class AnsiConsoleReporter(
  var name: String,
  override var reportDir: File?,
  var displayFullDiff: Boolean
) : VerifierReporter {

  constructor(name: String, reportDir: File?) : this(name, reportDir, false)

  override val ext: String? = null
  val t = TermColors()

  override var reportFile: File
    get() = TODO("not implemented")
    set(value) {}

  override fun includesMetadata() {
    println("      includes message metadata")
  }

  override fun metadataComparisonOk() {
    println("      has matching metadata (${t.green("OK")})")
  }

  override fun metadataComparisonOk(key: String, value: Any?) {
    println("        \"${t.bold(key)}\" with value \"${t.bold(value.toString())}\" (${t.green("OK")})")
  }

  override fun metadataComparisonFailed(key: String, value: Any?, comparison: Any) {
    println("        \"${t.bold(key)}\" with value \"${t.bold(value.toString())}\" (${t.red("FAILED")})")
  }

  override fun initialise(provider: IProviderInfo) { }

  override fun finaliseReport() { }

  override fun reportVerificationForConsumer(consumer: IConsumerInfo, provider: IProviderInfo, tag: String?) {
    var out = "\nVerifying a pact between ${t.bold(consumer.name)}"
    if (!consumer.name.contains(provider.name)) {
      out += " and ${t.bold(provider.name)}"
    }
    if (tag != null) {
      out += " for tag ${t.bold(tag)}"
    }
    println(out)
  }

  override fun verifyConsumerFromUrl(pactUrl: UrlPactSource, consumer: IConsumerInfo) {
    println("  [from ${pactUrl.description()}]")
  }

  override fun verifyConsumerFromFile(pactFile: PactSource, consumer: IConsumerInfo) {
    println("  [Using ${pactFile.description()}]")
  }

  override fun pactLoadFailureForConsumer(consumer: IConsumerInfo, message: String) { }

  override fun warnProviderHasNoConsumers(provider: IProviderInfo) {
    println("         ${t.yellow("WARNING: There are no consumers to verify for provider '${provider.name}'")}")
  }

  override fun warnPactFileHasNoInteractions(pact: Pact<Interaction>) {
    println("         ${t.yellow("WARNING: Pact file has no interactions")}")
  }

  override fun interactionDescription(interaction: Interaction) {
    println("  " + interaction.description)
  }

  override fun stateForInteraction(state: String, provider: IProviderInfo, consumer: IConsumerInfo, isSetup: Boolean) {
    println("  Given ${t.bold(state)}")
  }

  override fun warnStateChangeIgnored(state: String, IProviderInfo: IProviderInfo, IConsumerInfo: IConsumerInfo) {
    println("         ${t.yellow("WARNING: State Change ignored as there is no stateChange URL")}")
  }

  override fun stateChangeRequestFailedWithException(
    state: String,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    isSetup: Boolean,
    e: Exception,
    printStackTrace: Boolean
  ) {
    println("         ${t.red("State Change Request Failed - ${e.message}")}")
    if (printStackTrace) {
      e.printStackTrace()
    }
  }

  override fun stateChangeRequestFailed(state: String, provider: IProviderInfo, isSetup: Boolean, httpStatus: String) {
    println("         ${t.red("State Change Request Failed - $httpStatus")}")
  }

  override fun warnStateChangeIgnoredDueToInvalidUrl(
    state: String,
    provider: IProviderInfo,
    isSetup: Boolean,
    stateChangeHandler: Any
  ) {
    println("         ${t.yellow("WARNING: State Change ignored as there is no stateChange URL, " +
      "received \"$stateChangeHandler\"")}")
  }

  override fun requestFailed(
    provider: IProviderInfo,
    interaction: Interaction,
    interactionMessage: String,
    e: Exception,
    printStackTrace: Boolean
  ) {
    println("      ${t.red("Request Failed - ${e.message}")}")
    if (printStackTrace) {
      e.printStackTrace()
    }
  }

  override fun returnsAResponseWhich() {
    println("    returns a response which")
  }

  override fun statusComparisonOk(status: Int) {
    println("      has status code ${t.bold(status.toString())} (${t.green("OK")})")
  }

  override fun statusComparisonFailed(status: Int, comparison: Any) {
    println("      has status code ${t.bold(status.toString())} (${t.red("FAILED")})")
  }

  override fun includesHeaders() {
    println("      includes headers")
  }

  override fun headerComparisonOk(key: String, value: List<String>) {
    println("        \"${t.bold(key)}\" with value \"${t.bold(value.joinToString(", "))}\"" +
      " (${t.green("OK")})")
  }

  override fun headerComparisonFailed(key: String, value: List<String>, comparison: Any) {
    println("        \"${t.bold(key)}\" with value \"${t.bold(value.joinToString(", "))}\" " +
      "(${t.red("FAILED")})")
  }

  override fun bodyComparisonOk() {
    println("      has a matching body (${t.green("OK")})")
  }

  override fun bodyComparisonFailed(comparison: Any) {
    println("      has a matching body (${t.red("FAILED")})")
  }

  override fun errorHasNoAnnotatedMethodsFoundForInteraction(interaction: Interaction) { }

  override fun verificationFailed(interaction: Interaction, e: Exception, printStackTrace: Boolean) {
    println("      ${t.red("Verification Failed - ${e.message}")}")
    if (printStackTrace) {
      e.printStackTrace()
    }
  }

  override fun generatesAMessageWhich() {
    println("    generates a message which")
  }

  override fun displayFailures(failures: Map<String, Any>) {
    println("\nFailures:\n")
    failures.entries.forEachIndexed { i, err ->
      println("$i) ${err.key}")
      when {
        err.value is Throwable -> displayError(err.value as Throwable)
        err.value is Map<*, *> && (err.value as Map<*, *>).containsKey("comparison") &&
          (err.value as Map<*, *>)["comparison"] is Map<*, *> -> displayDiff(err.value as Map<String, Any>)
        err.value is String -> println("      ${err.value}")
        err.value is Map<*, *> -> (err.value as Map<*, *>).forEach { (key, message) ->
          println("      $key -> $message")
        }
        else -> println("      $err")
      }
      println()
    }
  }

  override fun displayFailures(failures: List<VerificationResult.Failed>) {
    println("\nFailures:\n")
    failures.forEachIndexed { i, err ->
      println("${i + 1}) ${err.verificationDescription}\n")
      err.failures.forEachIndexed { index, failure ->
        println("    ${i + 1}.${index + 1}) ${failure.formatForDisplay(t)}\n")
      }
    }
  }

  override fun reportVerificationNoticesForConsumer(
    consumer: IConsumerInfo,
    provider: IProviderInfo,
    notices: List<VerificationNotice>
  ) {
    println("\n  Notices:")
    notices.forEachIndexed { i, notice -> println("    ${i + 1}) ${notice.text}") }
    println()
  }

  override fun warnPublishResultsSkippedBecauseFiltered() {
    println(t.yellow("\nNOTE: Skipping publishing of verification results as the interactions have been filtered\n"))
  }

  override fun warnPublishResultsSkippedBecauseDisabled(envVar: String) {
    println(t.yellow("\nNOTE: Skipping publishing of verification results as it has been disabled " +
      "($envVar is not 'true')\n"))
  }

  private fun displayDiff(diff: Map<String, Any>) {
    (diff["comparison"] as Map<String, List<Map<String, Any>>>).forEach { (key, messageAndDiff) ->
      messageAndDiff.forEach { mismatch ->
        println("      $key -> ${mismatch["mismatch"]}")
        println()

        val mismatchDiff = if (mismatch["diff"] is List<*>) mismatch["diff"] as List<String>
          else listOf(mismatch["diff"].toString())
        if (mismatchDiff.any { it.isNotEmpty() }) {
          println("        Diff:")
          println()

          mismatchDiff.filter { it.isNotEmpty() }.forEach {
            it.split('\n').forEach { delta ->
              when {
                delta.startsWith('@') -> println("        ${t.cyan(delta)}")
                delta.startsWith('-') -> println("        ${t.red(delta)}")
                delta.startsWith('+') -> println("        ${t.green(delta)}")
                else -> println("        $delta")
              }
            }
            println()
          }
        }
      }
    }

    if (displayFullDiff) {
      println("      Full Diff:")
      println()

      (diff["diff"] as List<String>).forEach { delta ->
        when {
          delta.startsWith('@') -> println("        ${t.cyan(delta)}")
          delta.startsWith('-') -> println("        ${t.red(delta)}")
          delta.startsWith('+') -> println("        ${t.green(delta)}")
          else -> println("      $delta")
        }
      }
      println()
    }
  }

  private fun displayError(err: Throwable) {
    if (!err.message.isNullOrEmpty()) {
      err.message!!.split('\n').forEach {
        println("      $it")
      }
    } else {
      println("      ${err.javaClass.name}")
    }
  }
}
