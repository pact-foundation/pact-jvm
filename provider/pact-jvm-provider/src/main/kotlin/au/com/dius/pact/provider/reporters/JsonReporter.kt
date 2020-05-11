package au.com.dius.pact.provider.reporters

import arrow.core.Either
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
import au.com.dius.pact.core.support.property
import au.com.dius.pact.provider.BodyComparisonResult
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.VerificationResult
import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.isNotEmpty
import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.set
import com.github.salomonbrys.kotson.string
import com.github.salomonbrys.kotson.toJson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import org.apache.commons.lang3.exception.ExceptionUtils
import java.io.File
import java.time.ZonedDateTime

/**
 * Pact verifier reporter that generates the results of the verification in JSON format
 */
class JsonReporter(
  var name: String = "json",
  override var reportDir: File?,
  var jsonData: JsonObject = JsonObject(),
  override var ext: String = ".json",
  private var providerName: String? = null
) : VerifierReporter {

  constructor(name: String, reportDir: File?) : this(name, reportDir, JsonObject(), ".json", null)

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
      "execution" to jsonArray()
    )
    reportDir!!.mkdirs()
    reportFile = File(reportDir, providerName + ext)
  }

  override fun finaliseReport() {
    if (jsonData.isNotEmpty()) {
      when {
        reportFile.exists() && reportFile.length() > 0 -> {
          val existingContents = JsonParser.parseString(reportFile.readText())
          if (existingContents.isJsonObject && existingContents.obj.has("provider") &&
            providerName == existingContents["provider"].obj["name"].string) {
            existingContents["metaData"] = jsonData["metaData"]
            existingContents["execution"].array.addAll(jsonData["execution"].array)
            reportFile.writeText(existingContents.toString())
          } else {
            reportFile.writeText(jsonData.toString())
          }
        }
        else -> reportFile.writeText(jsonData.toString())
      }
    }
  }

  override fun reportVerificationForConsumer(consumer: IConsumerInfo, provider: IProviderInfo, tag: String?) {
    val jsonObject = jsonObject(
      "consumer" to jsonObject("name" to consumer.name),
      "interactions" to jsonArray(),
      "pending" to consumer.pending
    )
    if (tag.isNotEmpty()) {
      jsonObject.add("tag", JsonPrimitive(tag))
    }
    jsonData["execution"].array.add(jsonObject)
  }

  override fun verifyConsumerFromUrl(pactUrl: UrlPactSource, consumer: IConsumerInfo) {
    jsonData["execution"].array.last()["consumer"].obj["source"] = jsonObject("url" to pactUrl.url)
  }

  override fun verifyConsumerFromFile(pactFile: PactSource, consumer: IConsumerInfo) {
    jsonData["execution"].array.last()["consumer"].obj["source"] = jsonObject(
      "file" to if (pactFile is FileSource<*>) pactFile.file.toString() else pactFile.description()
    )
  }

  override fun pactLoadFailureForConsumer(consumer: IConsumerInfo, message: String) {
    if (jsonData["execution"].array.size() == 0) {
      jsonData["execution"].array.add(jsonObject(
        "consumer" to jsonObject("name" to consumer.name),
        "interactions" to jsonArray()
      ))
    }
    jsonData["execution"].array.last()["result"] = jsonObject(
      "state" to "Pact Load Failure",
      "message" to message
    )
  }

  override fun warnProviderHasNoConsumers(provider: IProviderInfo) { }

  override fun warnPactFileHasNoInteractions(pact: Pact<Interaction>) { }

  override fun interactionDescription(interaction: Interaction) {
    jsonData["execution"].array.last()["interactions"].array.add(jsonObject(
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
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    isSetup: Boolean,
    e: Exception,
    printStackTrace: Boolean
  ) {
    val interactions = jsonData["execution"].array.last()["interactions"].array
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
      interactions.last()["verification"] = error
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
    jsonData["execution"].array.last()["interactions"].array.last()["verification"] = jsonObject(
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
    val verification = jsonData["execution"].array.last()["interactions"].array.last()["verification"]
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
    val verification = jsonData["execution"].array.last()["interactions"].array.last()["verification"].obj
    verification["result"] = FAILED
    if (!verification.has("header")) {
      verification["header"] = jsonObject()
    }
    verification["header"].obj[key] = when (comparison) {
      is List<*> -> Json.toJson(comparison.map {
        when (it) {
          is HeaderMismatch -> it.mismatch.toJson()
          else -> Json.toJson(it)
        }
      })
      else -> Json.toJson(comparison)
    }
  }

  override fun bodyComparisonOk() { }

  override fun bodyComparisonFailed(comparison: Any) {
    val verification = jsonData["execution"].array.last()["interactions"].array.last()["verification"].obj
    verification["result"] = FAILED
    verification["body"] = when (comparison) {
      is Either.Left<*> -> Json.toJson((comparison as Either.Left<BodyTypeMismatch>).a.description())
      is Either.Right<*> -> (comparison as Either.Right<BodyComparisonResult>).b.toJson()
      else -> Json.toJson(comparison)
    }
  }

  override fun errorHasNoAnnotatedMethodsFoundForInteraction(interaction: Interaction) {
    jsonData["execution"].array.last()["interactions"].array.last()["verification"] = jsonObject(
      "result" to FAILED,
      "cause" to jsonObject("message" to "No Annotated Methods Found For Interaction")
    )
  }

  override fun verificationFailed(interaction: Interaction, e: Exception, printStackTrace: Boolean) {
    jsonData["execution"].array.last()["interactions"].array.last()["verification"] = jsonObject(
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
    val verification = jsonData["execution"].array.last()["interactions"].array.last()["verification"].obj
    verification["result"] = FAILED
    if (!verification.has("metadata")) {
      verification["metadata"] = jsonObject()
    }
    verification["metadata"].obj[key] = comparison
  }

  override fun includesMetadata() { }

  override fun metadataComparisonOk(key: String, value: Any?) { }

  override fun metadataComparisonOk() { }

  override fun reportVerificationNoticesForConsumer(
    consumer: IConsumerInfo,
    provider: IProviderInfo,
    notices: List<VerificationNotice>
  ) {
    jsonData["execution"].array.last()["consumer"]["notices"] = jsonArray(notices.map { it.text })
  }

  override fun warnPublishResultsSkippedBecauseFiltered() { }
  override fun warnPublishResultsSkippedBecauseDisabled(envVar: String) { }

  companion object {
    const val REPORT_FORMAT = "0.1.0"
    const val FAILED = "failed"
  }
}
