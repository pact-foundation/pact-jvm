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
import java.io.StringWriter
import java.time.ZonedDateTime

data class Event(
  val type: String,
  val contents: String,
  val data: List<Any?>
)

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

  private lateinit var provider: IProviderInfo
  private val events = mutableListOf<Event>()

  init {
    if (reportDir == null) {
      reportDir = File(System.getProperty("user.dir"))
    }
    reportFile = File(reportDir, "$name$ext")
  }

  override fun initialise(provider: IProviderInfo) {
    this.provider = provider
    reportDir!!.mkdirs()
    reportFile = File(reportDir, provider.name + ext)
  }

  override fun finaliseReport() {
    val pw = PrintWriter(BufferedWriter(FileWriter(reportFile, false)))
    pw.write("""
      # ${provider.name}
    
      | Description    | Value |
      | -------------- | ----- |
      | Date Generated | ${ZonedDateTime.now()} |
      | Pact Version   | ${BasePact.lookupVersion()} |
    
    """.trimIndent())

    for (event in events) {
      pw.write(event.contents)
    }
    pw.close()
  }

  override fun reportVerificationForConsumer(consumer: IConsumerInfo, provider: IProviderInfo, tag: String?) {
    val output = StringBuilder("## Verifying a pact between _${consumer.name}_")
    if (!consumer.name.contains(provider.name)) {
      output.append(" and _${provider.name}_")
    }
    if (tag != null) {
      output.append(" for tag $tag")
    }
    if (consumer.pending) {
      output.append(" [PENDING]")
    }
    output.append("\n\n")
    events.add(Event("reportVerificationForConsumer", output.toString(), listOf(consumer, provider, tag)))
  }

  override fun verifyConsumerFromUrl(pactUrl: UrlPactSource, consumer: IConsumerInfo) {
    events.add(Event("verifyConsumerFromUrl", "From `${pactUrl.description()}`<br/>\n",
      listOf(pactUrl, consumer)))
  }

  override fun verifyConsumerFromFile(pactFile: PactSource, consumer: IConsumerInfo) {
    events.add(Event("verifyConsumerFromFile", "From `${pactFile.description()}`<br/>\n",
      listOf(pactFile, consumer)))
  }

  override fun pactLoadFailureForConsumer(consumer: IConsumerInfo, message: String) { }

  override fun warnProviderHasNoConsumers(provider: IProviderInfo) { }

  override fun warnPactFileHasNoInteractions(pact: Pact<Interaction>) { }

  override fun interactionDescription(interaction: Interaction) {
    events.add(Event("interactionDescription", "${interaction.description}  \n", listOf(interaction)))
  }

  override fun stateForInteraction(state: String, provider: IProviderInfo, consumer: IConsumerInfo, isSetup: Boolean) {
    events.add(Event("stateForInteraction", "Given **$state**  \n",
      listOf(state, provider, consumer, isSetup)))
  }

  override fun warnStateChangeIgnored(state: String, provider: IProviderInfo, consumer: IConsumerInfo) {
    events.add(Event("warnStateChangeIgnored",
      "&nbsp;&nbsp;&nbsp;&nbsp;<span style=\'color: yellow\'>WARNING: State Change ignored as " +
      "there is no stateChange URL</span>  \n", listOf(state, provider, consumer)))
  }

  override fun stateChangeRequestFailedWithException(
    state: String,
    isSetup: Boolean,
    e: Exception,
    printStackTrace: Boolean
  ) {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    pw.write("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>State Change Request Failed - ${e.message}" +
      "</span>\n\n```\n")
    e.printStackTrace(pw)
    pw.write("\n```\n\n")
    pw.close()

    events.add(Event("stateChangeRequestFailedWithException", sw.toString(),
      listOf(state, isSetup, e, printStackTrace)))
  }

  override fun stateChangeRequestFailed(state: String, provider: IProviderInfo, isSetup: Boolean, httpStatus: String) {
    events.add(Event("stateChangeRequestFailedWithException",
      "&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>State Change Request Failed - $httpStatus" +
      "</span>  \n", listOf(state, provider, isSetup, httpStatus)))
  }

  override fun warnStateChangeIgnoredDueToInvalidUrl(
    state: String,
    provider: IProviderInfo,
    isSetup: Boolean,
    stateChangeHandler: Any
  ) {
    events.add(Event("warnStateChangeIgnoredDueToInvalidUrl",
      "&nbsp;&nbsp;&nbsp;&nbsp;<span style=\'color: yellow\'>WARNING: State Change ignored as " +
        "there is no stateChange URL, received `$stateChangeHandler`</span>  \n",
      listOf(state, provider, isSetup, stateChangeHandler)))
  }

  override fun requestFailed(
    provider: IProviderInfo,
    interaction: Interaction,
    interactionMessage: String,
    e: Exception,
    printStackTrace: Boolean
  ) {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    pw.write("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>Request Failed - ${e.message}</span>\n\n```\n")
    e.printStackTrace(pw)
    pw.write("\n```\n\n")
    pw.close()

    events.add(Event("requestFailed", sw.toString(), listOf(provider, interaction, interactionMessage, e,
      printStackTrace)))
  }

  override fun returnsAResponseWhich() {
    events.add(Event("returnsAResponseWhich", "&nbsp;&nbsp;returns a response which  \n", listOf()))
  }

  override fun statusComparisonOk(status: Int) {
    events.add(Event("statusComparisonOk", "&nbsp;&nbsp;&nbsp;&nbsp;has status code **$status** " +
      "(<span style='color:green'>OK</span>)  \n", listOf(status)))
  }

  override fun statusComparisonFailed(status: Int, comparison: Any) {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    pw.write("&nbsp;&nbsp;&nbsp;&nbsp;has status code **$status** " +
      "(<span style='color:red'>FAILED</span>)\n\n```\n")
    if (comparison.hasProperty("message")) {
      pw.write(comparison.property("message")?.get(comparison).toString())
    } else {
      pw.write(comparison.toString())
    }
    pw.write("\n```\n\n")
    pw.close()

    events.add(Event("statusComparisonFailed", sw.toString(), listOf(status, comparison)))
  }

  override fun includesHeaders() {
    events.add(Event("includesHeaders", "&nbsp;&nbsp;&nbsp;&nbsp;includes headers  \n", listOf()))
  }

  override fun headerComparisonOk(key: String, value: List<String>) {
    events.add(Event("headerComparisonOk",
      "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"**$key**\" with value \"**$value**\" " +
      "(<span style=\'color:green\'>OK</span>)  \n", listOf(key, value)))
  }

  override fun headerComparisonFailed(key: String, value: List<String>, comparison: Any) {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    pw.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"**$key**\" with value \"**$value**\" " +
      "(<span style=\'color:red\'>FAILED</span>)  \n\n```\n")
    when (comparison) {
      is List<*> -> comparison.forEach {
        when (it) {
          is HeaderMismatch -> pw.write(it.mismatch)
          else -> pw.write(it.toString())
        }
      }
      else -> pw.write(comparison.toString())
    }
    pw.write("\n```\n\n")
    pw.close()

    events.add(Event("headerComparisonFailed", sw.toString(), listOf(key, value, comparison)))
  }

  override fun bodyComparisonOk() {
    events.add(Event("bodyComparisonOk",
      "&nbsp;&nbsp;&nbsp;&nbsp;has a matching body (<span style='color:green'>OK</span>)  \n", listOf()))
  }

  override fun bodyComparisonFailed(comparison: Any) {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    pw.write("&nbsp;&nbsp;&nbsp;&nbsp;has a matching body (<span style='color:red'>FAILED</span>)  \n\n")

    when (comparison) {
      is Err<*> -> {
        comparison as Err<BodyTypeMismatch>
        pw.write("```\n${comparison.error.description()}\n```\n")
      }
      is Ok<*> -> {
        comparison as Ok<BodyComparisonResult>
        pw.write("| Path | Failure |\n")
        pw.write("| ---- | ------- |\n")
        comparison.value.mismatches.forEach { entry ->
          pw.write("|`${entry.key}`|${entry.value.joinToString("\n") { it.description() }}|\n")
        }
        pw.write("\n\nDiff:\n\n")
        renderDiff(pw, comparison.value.diff)
        pw.write("\n\n")
      }
      else -> pw.write("```\n${comparison}\n```\n")
    }
    pw.close()
    events.add(Event("bodyComparisonFailed", sw.toString(), listOf(comparison)))
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
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    pw.write("&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>Verification Failed - ${e.message}</span>\n\n```\n")
    e.printStackTrace(pw)
    pw.write("\n```\n\n")
    pw.close()
    events.add(Event("verificationFailed", sw.toString(), listOf(interaction, e, printStackTrace)))
  }

  override fun generatesAMessageWhich() {
    events.add(Event("generatesAMessageWhich", "&nbsp;&nbsp;generates a message which  \n", listOf()))
  }

  override fun displayFailures(failures: Map<String, Any>) { }

  override fun displayFailures(failures: List<VerificationResult.Failed>) { }

  override fun metadataComparisonFailed(key: String, value: Any?, comparison: Any) {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    pw.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"**$key**\" with value \"**$value**\" " +
      "(<span style=\'color:red\'>FAILED</span>)  \n")
    pw.write("\n```\n$comparison\n```\n\n")
    pw.close()
    events.add(Event("metadataComparisonFailed", sw.toString(), listOf(key, value, comparison)))
  }

  override fun includesMetadata() {
    events.add(Event("includesMetadata", "&nbsp;&nbsp;&nbsp;&nbsp;includes metadata  \n", listOf()))
  }

  override fun metadataComparisonOk(key: String, value: Any?) {
    events.add(Event("metadataComparisonOk",
      "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"**$key**\" with value \"**$value**\" " +
      "(<span style=\'color:green\'>OK</span>)  \n", listOf(key, value)))
  }

  override fun metadataComparisonOk() {
    events.add(Event("metadataComparisonOk",
      "&nbsp;&nbsp;&nbsp;&nbsp;has matching metadata (<span style='color:green'>OK</span>)\n", listOf()))
  }

  override fun reportVerificationNoticesForConsumer(
    consumer: IConsumerInfo,
    provider: IProviderInfo,
    notices: List<VerificationNotice>
  ) {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    pw.write("Notices:\n")
    notices.forEachIndexed { i, notice -> pw.write("${i + 1}. ${notice.text}\n") }
    pw.write("\n")
    pw.close()
    events.add(Event("reportVerificationNoticesForConsumer", sw.toString(), listOf(consumer, provider, notices)))
  }

  override fun warnPublishResultsSkippedBecauseFiltered() {
    events.add(Event("warnPublishResultsSkippedBecauseFiltered",
      "NOTE: Skipping publishing of verification results as the interactions have been filtered\n", listOf()))
  }

  override fun warnPublishResultsSkippedBecauseDisabled(envVar: String) {
    events.add(Event("warnPublishResultsSkippedBecauseDisabled",
      "NOTE: Skipping publishing of verification results as it has been disabled ($envVar is not 'true')\n",
      listOf(envVar)))
  }
}
