package au.com.dius.pact.provider.reporters

import au.com.dius.pact.core.matchers.BodyTypeMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.UrlPactSource
import au.com.dius.pact.core.pactbroker.VerificationNotice
import au.com.dius.pact.core.support.hasProperty
import au.com.dius.pact.core.support.property
import au.com.dius.pact.provider.BodyComparisonResult
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.VerificationResult
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.ZonedDateTime

/**
 * Pact verifier reporter that displays the results of the verification in a markdown document
 */
@Suppress("EmptyFunctionBlock", "TooManyFunctions")
class MarkdownReporter(
  var name: String,
  override var reportDir: File?,
  override var ext: String
) : VerifierReporter {

  constructor(name: String, reportDir: File?) : this(name, reportDir, ".md")

  override lateinit var reportFile: File
  override lateinit var verifier: IProviderVerifier

  init {
    if (reportDir == null) {
      reportDir = File(System.getProperty("user.dir"))
    }
    reportFile = File(reportDir, "$name$ext")
  }

  private var pw: PrintWriter? = null

  override fun initialise(provider: IProviderInfo) {
    pw?.close()
    reportDir!!.mkdirs()
    reportFile = File(reportDir, provider.name + ext)
    pw = PrintWriter(BufferedWriter(FileWriter(reportFile, true)))
    pw!!.write("""
      # ${provider.name}
    
      | Description    | Value |
      | -------------- | ----- |
      | Date Generated | ${ZonedDateTime.now()} |
      | Pact Version   | ${BasePact.lookupVersion()} |
    
    """.trimIndent())
  }

  override fun finaliseReport() {
    pw?.close()
  }

  override fun reportVerificationForConsumer(consumer: IConsumerInfo, provider: IProviderInfo, tag: String?) {
    val report = StringBuilder("## Verifying a pact between _${consumer.name}_")
    if (!consumer.name.contains(provider.name)) {
      report.append(" and _${provider.name}_")
    }
    if (tag != null) {
      report.append(" for tag $tag")
    }
    if (consumer.pending) {
      report.append(" [PENDING]")
    }
    report.append("\n\n")
    pw!!.write(report.toString())
  }

  override fun verifyConsumerFromUrl(pactUrl: UrlPactSource, consumer: IConsumerInfo) {
    pw!!.write("From `${pactUrl.description()}`<br/>\n")
  }

  override fun verifyConsumerFromFile(pactFile: PactSource, consumer: IConsumerInfo) {
    pw!!.write("From `${pactFile.description()}`<br/>\n")
  }

  override fun pactLoadFailureForConsumer(consumer: IConsumerInfo, message: String) { }

  override fun warnProviderHasNoConsumers(provider: IProviderInfo) { }

  override fun warnPactFileHasNoInteractions(pact: Pact<Interaction>) { }

  override fun interactionDescription(interaction: Interaction) {
    pw!!.write("${interaction.description}  \n")
  }

  override fun stateForInteraction(state: String, provider: IProviderInfo, consumer: IConsumerInfo, isSetup: Boolean) {
    pw!!.write("Given **$state**  \n")
  }

  override fun warnStateChangeIgnored(state: String, provider: IProviderInfo, consumer: IConsumerInfo) {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;<span style=\'color: yellow\'>WARNING: State Change ignored as " +
      "there is no stateChange URL</span>  \n")
  }

  override fun stateChangeRequestFailedWithException(
    state: String,
    isSetup: Boolean,
    e: Exception,
    printStackTrace: Boolean
  ) {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>State Change Request Failed - ${e.message}" +
      "</span>\n\n```\n")
    e.printStackTrace(pw!!)
    pw!!.write("\n```\n\n")
  }

  override fun stateChangeRequestFailed(state: String, provider: IProviderInfo, isSetup: Boolean, httpStatus: String) {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>State Change Request Failed - $httpStatus" +
      "</span>  \n")
  }

  override fun warnStateChangeIgnoredDueToInvalidUrl(
    state: String,
    provider: IProviderInfo,
    isSetup: Boolean,
    stateChangeHandler: Any
  ) {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;<span style=\'color: yellow\'>WARNING: State Change ignored as " +
      "there is no stateChange URL, received `$stateChangeHandler`</span>  \n")
  }

  override fun requestFailed(
    provider: IProviderInfo,
    interaction: Interaction,
    interactionMessage: String,
    e: Exception,
    printStackTrace: Boolean
  ) {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>Request Failed - ${e.message}</span>\n\n```\n")
    e.printStackTrace(pw!!)
    pw!!.write("\n```\n\n")
  }

  override fun returnsAResponseWhich() {
    pw!!.write("&nbsp;&nbsp;returns a response which  \n")
  }

  override fun statusComparisonOk(status: Int) {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;has status code **$status** " +
      "(<span style='color:green'>OK</span>)  \n")
  }

  override fun statusComparisonFailed(status: Int, comparison: Any) {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;has status code **$status** " +
      "(<span style='color:red'>FAILED</span>)\n\n```\n")
    if (comparison.hasProperty("message")) {
      pw!!.write(comparison.property("message")?.get(comparison).toString())
    } else {
      pw!!.write(comparison.toString())
    }
    pw!!.write("\n```\n\n")
  }

  override fun includesHeaders() {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;includes headers  \n")
  }

  override fun headerComparisonOk(key: String, value: List<String>) {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"**$key**\" with value \"**$value**\" " +
      "(<span style=\'color:green\'>OK</span>)  \n")
  }

  override fun headerComparisonFailed(key: String, value: List<String>, comparison: Any) {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"**$key**\" with value \"**$value**\" " +
      "(<span style=\'color:red\'>FAILED</span>)  \n\n```\n")
    when (comparison) {
      is List<*> -> comparison.forEach {
        when (it) {
          is HeaderMismatch -> pw!!.write(it.mismatch)
          else -> pw!!.write(it.toString())
        }
      }
      else -> pw!!.write(comparison.toString())
    }
    pw!!.write("\n```\n\n")
  }

  override fun bodyComparisonOk() {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;has a matching body (<span style='color:green'>OK</span>)  \n")
  }

  override fun bodyComparisonFailed(comparison: Any) {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;has a matching body (<span style='color:red'>FAILED</span>)  \n\n")

    when (comparison) {
      is Err<*> -> {
        comparison as Err<BodyTypeMismatch>
        pw!!.write("```\n${comparison.error.description()}\n```\n")
      }
      is Ok<*> -> {
        comparison as Ok<BodyComparisonResult>
        pw!!.write("| Path | Failure |\n")
        pw!!.write("| ---- | ------- |\n")
        comparison.value.mismatches.forEach { entry ->
          pw!!.write("|`${entry.key}`|${entry.value.joinToString("\n") { it.description() }}|\n")
        }
        pw!!.write("\n\nDiff:\n\n")
        renderDiff(pw!!, comparison.value.diff)
        pw!!.write("\n\n")
      }
      else -> pw!!.write("```\n${comparison}\n```\n")
    }
  }

  private fun renderDiff(pw: PrintWriter, diff: Any?) {
    pw.write("```diff\n")
    if (diff is List<*>) {
      pw.write(diff.joinToString("\n"))
    } else {
      pw.write(diff.toString())
    }
    pw.write("\n```\n")
  }

  override fun errorHasNoAnnotatedMethodsFoundForInteraction(interaction: Interaction) { }

  override fun verificationFailed(interaction: Interaction, e: Exception, printStackTrace: Boolean) {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>Verification Failed - ${e.message}</span>\n\n```\n")
    e.printStackTrace(pw)
    pw!!.write("\n```\n\n")
  }

  override fun generatesAMessageWhich() {
    pw!!.write("&nbsp;&nbsp;generates a message which  \n")
  }

  override fun displayFailures(failures: Map<String, Any>) { }

  override fun displayFailures(failures: List<VerificationResult.Failed>) { }

  override fun metadataComparisonFailed(key: String, value: Any?, comparison: Any) {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"**$key**\" with value \"**$value**\" " +
      "(<span style=\'color:red\'>FAILED</span>)  \n")
    pw!!.write("\n```\n$comparison\n```\n\n")
  }

  override fun includesMetadata() {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;includes metadata  \n")
  }

  override fun metadataComparisonOk(key: String, value: Any?) {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"**$key**\" with value \"**$value**\" " +
      "(<span style=\'color:green\'>OK</span>)  \n")
  }

  override fun metadataComparisonOk() {
    pw!!.write("&nbsp;&nbsp;&nbsp;&nbsp;has matching metadata (<span style='color:green'>OK</span>)\n")
  }

  override fun reportVerificationNoticesForConsumer(
    consumer: IConsumerInfo,
    provider: IProviderInfo,
    notices: List<VerificationNotice>
  ) {
    pw!!.write("Notices:\n")
    notices.forEachIndexed { i, notice -> pw!!.write("${i + 1}. ${notice.text}\n") }
    pw!!.write("\n")
  }

  override fun warnPublishResultsSkippedBecauseFiltered() {
    pw!!.write("NOTE: Skipping publishing of verification results as the interactions have been filtered\n")
  }

  override fun warnPublishResultsSkippedBecauseDisabled(envVar: String) {
    pw!!.write("NOTE: Skipping publishing of verification results as it has been disabled ($envVar is not 'true')\n")
  }
}
