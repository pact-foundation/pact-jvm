package au.com.dius.pact.provider.reporters

import au.com.dius.pact.core.matchers.BodyTypeMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.FileSource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.UrlPactSource
import au.com.dius.pact.core.pactbroker.VerificationNotice
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.hasProperty
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonToken
import au.com.dius.pact.core.support.json.JsonValue
import au.com.dius.pact.core.support.jsonArray
import au.com.dius.pact.core.support.jsonObject
import au.com.dius.pact.core.support.property
import au.com.dius.pact.provider.BodyComparisonResult
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.VerificationResult
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.apache.commons.lang3.exception.ExceptionUtils
import java.io.File
import java.time.ZonedDateTime

/**
 * Pact verifier reporter that generates the results of the verification in JSON format
 */
@Suppress("EmptyFunctionBlock", "TooManyFunctions")
class JsonReporter(
  var name: String = "json",
  override var reportDir: File?,
  var jsonData: JsonValue.Object = JsonValue.Object(),
  override var ext: String = ".json",
  private var providerName: String? = null
) : BaseVerifierReporter() {

  constructor(name: String, reportDir: File?) : this(name, reportDir, JsonValue.Object(), ".json", null)

  override lateinit var reportFile: File
  override lateinit var verifier: IProviderVerifier

  init {
    if (reportDir == null) {
      reportDir = File(System.getProperty("user.dir"))
    }
    reportFile = File(reportDir, "$name$ext")
  }

  override fun initialise(provider: IProviderInfo) {
    providerName = provider.name
    jsonData = jsonObject(
      "metaData" to jsonObject(
        "date" to ZonedDateTime.now().toString(),
        "pactJvmVersion" to BasePact.lookupVersion(),
        "reportFormat" to REPORT_FORMAT
      ),
      "provider" to jsonObject("name" to providerName),
      "execution" to JsonValue.Array()
    )
    reportDir!!.mkdirs()
    reportFile = File(reportDir, providerName + ext)
  }

  override fun finaliseReport() {
    if (jsonData.isNotEmpty()) {
      when {
        reportFile.exists() && reportFile.length() > 0 -> {
          val existingContents = JsonParser.parseString(reportFile.readText())
          if (existingContents is JsonValue.Object && existingContents.has("provider") &&
            providerName == existingContents["provider"]["name"].asString()) {
            existingContents["metaData"] = jsonData["metaData"]
            existingContents["execution"].asArray()!!.addAll(jsonData["execution"])
            reportFile.writeText(existingContents.serialise())
          } else {
            reportFile.writeText(jsonData.serialise())
          }
        }
        else -> reportFile.writeText(jsonData.serialise())
      }
    }
  }

  override fun reportVerificationForConsumer(consumer: IConsumerInfo, provider: IProviderInfo, tag: String?) {
    val jsonObject = jsonObject(
      "consumer" to jsonObject("name" to consumer.name),
      "interactions" to JsonValue.Array(),
      "pending" to consumer.pending
    )
    if (tag.isNotEmpty()) {
      jsonObject.add("tag", JsonValue.StringValue(JsonToken.StringValue(tag!!.toCharArray())))
    }
    jsonData["execution"].add(jsonObject)
  }

  override fun verifyConsumerFromUrl(pactUrl: UrlPactSource, consumer: IConsumerInfo) {
    jsonData["execution"].asArray()!!.last()["consumer"].asObject()!!["source"] = jsonObject("url" to pactUrl.url)
  }

  override fun verifyConsumerFromFile(pactFile: PactSource, consumer: IConsumerInfo) {
    jsonData["execution"].asArray()!!.last()["consumer"].asObject()!!["source"] = jsonObject(
      "file" to if (pactFile is FileSource) pactFile.file.toString() else pactFile.description()
    )
  }

  override fun pactLoadFailureForConsumer(consumer: IConsumerInfo, message: String) {
    if (jsonData["execution"].size() == 0) {
      jsonData["execution"].add(jsonObject(
        "consumer" to jsonObject("name" to consumer.name),
        "interactions" to JsonValue.Array()
      ))
    }
    jsonData["execution"].asArray()!!.last().asObject()!!["result"] = jsonObject(
      "state" to "Pact Load Failure",
      "message" to message
    )
  }

  override fun warnProviderHasNoConsumers(provider: IProviderInfo) { }

  override fun warnPactFileHasNoInteractions(pact: Pact) { }

  override fun interactionDescription(interaction: Interaction) {
    jsonData["execution"].asArray()!!.last()["interactions"].asArray()!!.add(jsonObject(
      "interaction" to Json.toJson(interaction.toMap(PactSpecVersion.V3)),
      "verification" to jsonObject("result" to "OK")
    ))
  }

  override fun stateForInteraction(
    state: String,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    isSetup: Boolean
  ) { }

  override fun warnStateChangeIgnored(state: String, provider: IProviderInfo, consumer: IConsumerInfo) { }

  override fun stateChangeRequestFailedWithException(
    state: String,
    isSetup: Boolean,
    e: Exception,
    printStackTrace: Boolean
  ) {
    val interactions = jsonData["execution"].asArray()!!.last()["interactions"].asArray()!!
    val error = jsonObject(
      "result" to FAILED,
      "message" to "State change '$state' callback failed",
      "exception" to jsonObject(
        "message" to e.message,
        "stackTrace" to jsonArray(ExceptionUtils.getStackFrames(e).toList())
      )
    )
    if (interactions.size() == 0) {
      interactions.add(jsonObject(
        "verification" to error
      ))
    } else {
      interactions.last().asObject()!!["verification"] = error
    }
  }

  override fun stateChangeRequestFailed(state: String, provider: IProviderInfo, isSetup: Boolean, httpStatus: String) {
  }

  override fun warnStateChangeIgnoredDueToInvalidUrl(
    state: String,
    provider: IProviderInfo,
    isSetup: Boolean,
    stateChangeHandler: Any
  ) { }

  override fun requestFailed(
    provider: IProviderInfo,
    interaction: Interaction,
    interactionMessage: String,
    e: Exception,
    printStackTrace: Boolean
  ) {
    jsonData["execution"].asArray()!!.last()["interactions"].asArray()!!.last().asObject()!!["verification"] =
      jsonObject(
        "result" to FAILED,
        "message" to interactionMessage,
        "exception" to jsonObject(
          "message" to e.message,
          "stackTrace" to jsonArray(ExceptionUtils.getStackFrames(e).toList())
        )
      )
  }

  override fun returnsAResponseWhich() { }

  override fun statusComparisonOk(status: Int) { }

  override fun statusComparisonFailed(status: Int, comparison: Any) {
    val verification = jsonData["execution"].asArray()!!.last()["interactions"].asArray()!!.last()["verification"]
      .asObject()!!
    verification["result"] = FAILED
    val statusJson = jsonArray(
      if (comparison.hasProperty("message")) {
        comparison.property("message")?.get(comparison).toString().split('\n')
      } else {
        comparison.toString().split('\n')
      }
    )
    verification["status"] = statusJson
  }

  override fun includesHeaders() { }

  override fun headerComparisonOk(key: String, value: List<String>) { }

  override fun headerComparisonFailed(key: String, value: List<String>, comparison: Any) {
    val verification = jsonData["execution"].asArray()!!.last()["interactions"].asArray()!!.last()["verification"]
      .asObject()!!
    verification["result"] = FAILED
    if (!verification.has("header")) {
      verification["header"] = jsonObject()
    }
    verification["header"].asObject()!![key] = when (comparison) {
      is List<*> -> Json.toJson(comparison.map {
        when (it) {
          is HeaderMismatch -> JsonValue.StringValue(JsonToken.StringValue(it.mismatch.toCharArray()))
          else -> Json.toJson(it)
        }
      })
      else -> Json.toJson(comparison)
    }
  }

  override fun bodyComparisonOk() { }

  override fun bodyComparisonFailed(comparison: Any) {
    val verification = jsonData["execution"].asArray()!!.last()["interactions"].asArray()!!.last()["verification"]
      .asObject()!!
    verification["result"] = FAILED
    verification["body"] = when (comparison) {
      is Err<*> -> Json.toJson((comparison as Err<BodyTypeMismatch>).error.description())
      is Ok<*> -> (comparison as Ok<BodyComparisonResult>).value.toJson()
      else -> Json.toJson(comparison)
    }
  }

  override fun errorHasNoAnnotatedMethodsFoundForInteraction(interaction: Interaction) {
    jsonData["execution"].asArray()!!.last()["interactions"].asArray()!!.last().asObject()!!["verification"] =
      jsonObject(
        "result" to FAILED,
        "cause" to jsonObject("message" to "No Annotated Methods Found For Interaction")
      )
  }

  override fun verificationFailed(interaction: Interaction, e: Exception, printStackTrace: Boolean) {
    jsonData["execution"].asArray()!!.last()["interactions"].asArray()!!.last().asObject()!!["verification"] =
      jsonObject(
        "result" to FAILED,
        "exception" to jsonObject(
          "message" to e.message,
          "stackTrace" to ExceptionUtils.getStackFrames(e)
      )
    )
  }

  override fun generatesAMessageWhich() { }

  override fun displayFailures(failures: Map<String, Any>) { }

  override fun displayFailures(failures: List<VerificationResult.Failed>) { }

  override fun metadataComparisonFailed(key: String, value: Any?, comparison: Any) {
    val verification = jsonData["execution"].asArray()!!.last()["interactions"].asArray()!!.last()["verification"]
      .asObject()!!
    verification["result"] = FAILED
    if (!verification.has("metadata")) {
      verification["metadata"] = jsonObject()
    }
    verification["metadata"].asObject()!![key] = comparison
  }

  override fun includesMetadata() { }

  override fun metadataComparisonOk(key: String, value: Any?) { }

  override fun metadataComparisonOk() { }

  override fun reportVerificationNoticesForConsumer(
    consumer: IConsumerInfo,
    provider: IProviderInfo,
    notices: List<VerificationNotice>
  ) {
    jsonData["execution"].asArray()!!.last()["consumer"].asObject()!!["notices"] = jsonArray(notices.map { it.text })
  }

  override fun warnPublishResultsSkippedBecauseFiltered() { }
  override fun warnPublishResultsSkippedBecauseDisabled(envVar: String) { }

  override fun receive(event: Event) {
    when (event) {
      is Event.DisplayInteractionComments ->
        jsonData["execution"].asArray()!!.last()["consumer"].asObject()!!["comments"] =
          JsonValue.Object(event.comments.toMutableMap())
      else -> super.receive(event)
    }
  }

  companion object {
    const val REPORT_FORMAT = "0.1.0"
    const val FAILED = "failed"
  }
}
