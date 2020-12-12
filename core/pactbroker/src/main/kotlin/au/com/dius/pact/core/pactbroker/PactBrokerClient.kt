package au.com.dius.pact.core.pactbroker

import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.handleWith
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import au.com.dius.pact.core.support.json.map
import au.com.dius.pact.core.support.jsonArray
import au.com.dius.pact.core.support.jsonObject
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.unwrap
import com.google.common.net.UrlEscapers.urlPathSegmentEscaper
import mu.KLogging
import java.io.File
import java.net.URLDecoder
import java.util.function.Consumer

/**
 * Wraps the response for a Pact from the broker with the link data associated with the Pact document.
 */
data class PactResponse(val pactFile: JsonValue.Object, val links: Map<String, Any?>)

sealed class TestResult {
  data class Ok(val interactionId: String? = null) : TestResult() {
    override fun toBoolean() = true

    override fun merge(result: TestResult) = when (result) {
      is Ok -> this.copy(interactionId = interactionId ?: result.interactionId)
      is Failed -> result.merge(this)
    }
  }

  data class Failed(var results: List<Map<String, Any?>> = emptyList(), val description: String = "") : TestResult() {
    override fun toBoolean() = false

    override fun merge(result: TestResult) = when (result) {
      is Ok -> this
      is Failed -> Failed(results + result.results, when {
        description.isNotEmpty() && result.description.isNotEmpty() && description != result.description ->
          "$description, ${result.description}"
        description.isNotEmpty() -> description
        else -> result.description
      })
    }
  }

  abstract fun toBoolean(): Boolean
  abstract fun merge(result: TestResult): TestResult
}

sealed class Latest {
  data class UseLatest(val latest: Boolean) : Latest()
  data class UseLatestTag(val latestTag: String) : Latest()
}

data class CanIDeployResult(val ok: Boolean, val message: String, val reason: String, val unknown: Int? = null)

/**
 * Consumer version selector. See https://docs.pact.io/pact_broker/advanced_topics/selectors
 */
data class ConsumerVersionSelector(
  val tag: String? = null,
  val latest: Boolean = true,
  val consumer: String? = null,
  val fallbackTag: String? = null
) {
  fun toJson(): JsonValue {
    val obj = JsonValue.Object("latest" to Json.toJson(latest))
    if (tag.isNotEmpty()) {
      obj.add("tag", Json.toJson(tag))
    }
    if (consumer.isNotEmpty()) {
      obj.add("consumer", Json.toJson(consumer))
    }
    if (fallbackTag.isNotEmpty()) {
      obj.add("fallbackTag", Json.toJson(fallbackTag))
    }
    return obj
  }
}

interface IPactBrokerClient {
  /**
   * Fetches all consumers for the given provider and selectors
   */
  fun fetchConsumersWithSelectors(
    providerName: String,
    selectors: List<ConsumerVersionSelector>,
    providerTags: List<String> = emptyList(),
    enablePending: Boolean = false,
    includeWipPactsSince: String
  ): Result<List<PactBrokerResult>, Exception>

  fun getUrlForProvider(providerName: String, tag: String): String?

  val options: Map<String, Any>
}

data class PactBrokerClientConfig(
  val retryCountWhileUnknown: Int = 0,
  val retryWhileUnknownInterval: Int = 10
)

/**
 * Client for the pact broker service
 */
open class PactBrokerClient(
  val pactBrokerUrl: String,
  @Deprecated("Move use of options to PactBrokerClientConfig")
  override val options: MutableMap<String, Any>,
  val config: PactBrokerClientConfig = PactBrokerClientConfig()
) : IPactBrokerClient {

  constructor(pactBrokerUrl: String) : this(pactBrokerUrl, mutableMapOf())

  /**
   * Fetches all consumers for the given provider
   */
  @Deprecated(message = "Use the version that takes selectors instead",
    replaceWith = ReplaceWith("fetchConsumersWithSelectors"))
  open fun fetchConsumers(provider: String): List<PactBrokerResult> {
    return try {
      val halClient = newHalClient()
      val consumers = mutableListOf<PactBrokerResult>()
      halClient.navigate(mapOf("provider" to provider), LATEST_PROVIDER_PACTS).forAll(PACTS, Consumer { pact ->
        val href = pact["href"].toString()
        val name = pact["name"].toString()
        if (options.containsKey("authentication")) {
          consumers.add(PactBrokerResult(name, href, pactBrokerUrl, options["authentication"] as List<String>))
        } else {
          consumers.add(PactBrokerResult(name, href, pactBrokerUrl))
        }
      })
      consumers
    } catch (e: NotFoundHalResponse) {
      // This means the provider is not defined in the broker, so fail gracefully.
      emptyList()
    }
  }

  /**
   * Fetches all consumers for the given provider and tag
   */
  @Deprecated(message = "Use fetchConsumersWithSelectors")
  open fun fetchConsumersWithTag(provider: String, tag: String): List<PactBrokerResult> {
    return try {
      val halClient = newHalClient()
      val consumers = mutableListOf<PactBrokerResult>()
      halClient.navigate(mapOf("provider" to provider, "tag" to tag), LATEST_PROVIDER_PACTS_WITH_TAG)
        .forAll(PACTS, Consumer { pact ->
        val href = pact["href"].toString()
        val name = pact["name"].toString()
        if (options.containsKey("authentication")) {
          consumers.add(PactBrokerResult(name, href, pactBrokerUrl, options["authentication"] as List<String>, tag = tag))
        } else {
          consumers.add(PactBrokerResult(name, href, pactBrokerUrl, emptyList(), tag = tag))
        }
      })
      consumers
    } catch (e: NotFoundHalResponse) {
      // This means the provider is not defined in the broker, so fail gracefully.
      emptyList()
    }
  }

  override fun fetchConsumersWithSelectors(
    providerName: String,
    selectors: List<ConsumerVersionSelector>,
    providerTags: List<String>,
    enablePending: Boolean,
    includeWipPactsSince: String
  ): Result<List<PactBrokerResult>, Exception> {
    val halClient = when (val navigateResult = handleWith<IHalClient> { newHalClient().navigate() }) {
      is Err<Exception> -> return navigateResult
      is Ok<IHalClient> -> navigateResult.value
    }
    val pactsForVerification = when {
      halClient.linkUrl(PROVIDER_PACTS_FOR_VERIFICATION) != null -> PROVIDER_PACTS_FOR_VERIFICATION
      halClient.linkUrl(BETA_PROVIDER_PACTS_FOR_VERIFICATION) != null -> BETA_PROVIDER_PACTS_FOR_VERIFICATION
      else -> null
    }
    return if (pactsForVerification != null) {
      fetchPactsUsingNewEndpoint(selectors, enablePending, providerTags, includeWipPactsSince, halClient, pactsForVerification, providerName)
    } else {
      handleWith {
        val tags = selectors.mapNotNull { it.tag }
        if (tags.isEmpty()) {
          fetchConsumers(providerName)
        } else {
          tags.flatMap { fetchConsumersWithTag(providerName, it) }
        }
      }
    }
  }

  private fun fetchPactsUsingNewEndpoint(
    selectors: List<ConsumerVersionSelector>,
    enablePending: Boolean,
    providerTags: List<String>,
    includeWipPactsSince: String,
    halClient: IHalClient,
    pactsForVerification: String,
    providerName: String
  ): Result<List<PactBrokerResult>, Exception> {
    val body = JsonValue.Object(
      "consumerVersionSelectors" to jsonArray(selectors.map { it.toJson() })
    )
    if (enablePending) {
      body["providerVersionTags"] = jsonArray(providerTags)
      body["includePendingStatus"] = true
      if (!includeWipPactsSince.isBlank()) {
        body["includeWipPactsSince"] = includeWipPactsSince
      }
    }

    return handleWith {
      halClient.postJson(pactsForVerification, mapOf("provider" to providerName), body.serialise()).map { result ->
        result["_embedded"]["pacts"].asArray().map { pactJson ->
          val selfLink = pactJson["_links"]["self"]
          val href = Json.toString(selfLink["href"])
          val name = Json.toString(selfLink["name"])
          val properties = pactJson["verificationProperties"]
          val notices = properties["notices"].asArray()
            .map { VerificationNotice.fromJson(it.asObject()) }
          var pending = false
          if (properties is JsonValue.Object && properties.has("pending") && properties["pending"].isBoolean) {
            pending = properties["pending"].asBoolean()
          }
          val wip = if (properties.has("wip") && properties["wip"].isBoolean) properties["wip"].asBoolean()
          else false
          if (options.containsKey("authentication")) {
            PactBrokerResult(name, href, pactBrokerUrl, options["authentication"] as List<String>, notices, pending,
              wip = wip, usedNewEndpoint = true)
          } else {
            PactBrokerResult(name, href, pactBrokerUrl, emptyList(), notices, pending, wip = wip,
              usedNewEndpoint = true)
          }
        }
      }
    }
  }

  /**
   * Uploads the given pact file to the broker, and optionally applies any tags
   */
  @JvmOverloads
  open fun uploadPactFile(
    pactFile: File,
    version: String,
    tags: List<String> = emptyList()
  ): Result<String?, Exception> {
    val pactText = pactFile.readText()
    val pact = JsonParser.parseString(pactText)
    val halClient = newHalClient()
    val providerName = pact["provider"]["name"].asString()
    val consumerName = pact["consumer"]["name"].asString()
    if (tags.isNotEmpty()) {
      uploadTags(halClient, consumerName, version, tags)
    }
    return halClient.navigate().putJson("pb:publish-pact", mapOf(
      "provider" to providerName,
      "consumer" to consumerName,
      "consumerApplicationVersion" to version
    ), pactText)
  }

  override fun getUrlForProvider(providerName: String, tag: String): String? {
    val halClient = newHalClient()
    if (tag.isEmpty() || tag == "latest") {
      halClient.navigate(mapOf("provider" to providerName), LATEST_PROVIDER_PACTS)
    } else {
      halClient.navigate(mapOf("provider" to providerName, "tag" to tag), LATEST_PROVIDER_PACTS_WITH_TAG)
    }
    return halClient.linkUrl(PACTS)
  }

  open fun fetchPact(url: String, encodePath: Boolean = true): PactResponse {
    val halDoc = newHalClient().fetch(url, encodePath).unwrap()
    return PactResponse(halDoc, HalClient.asMap(halDoc["_links"].asObject()))
  }

  open fun newHalClient(): IHalClient = HalClient(pactBrokerUrl, options)

  /**
   * Publishes the result to the "pb:publish-verification-results" link in the document attributes.
   */
  @JvmOverloads
  open fun publishVerificationResults(
    docAttributes: Map<String, Any?>,
    result: TestResult,
    version: String,
    buildUrl: String? = null
  ): Result<Boolean, String> {
    val halClient = newHalClient()
    val publishLink = docAttributes.mapKeys { it.key.toLowerCase() } ["pb:publish-verification-results"] // ktlint-disable curly-spacing
    return if (publishLink is Map<*, *>) {
      val jsonObject = buildPayload(result, version, buildUrl)
      val lowercaseMap = publishLink.mapKeys { it.key.toString().toLowerCase() }
      if (lowercaseMap.containsKey("href")) {
        halClient.postJson(lowercaseMap["href"].toString(), jsonObject.serialise()).mapError {
          logger.error(it) { "Publishing verification results failed with an exception" }
          "Publishing verification results failed with an exception: ${it.message}"
        }
      } else {
        Err("Unable to publish verification results as there is no pb:publish-verification-results link")
      }
    } else {
      Err("Unable to publish verification results as there is no pb:publish-verification-results link")
    }
  }

  fun buildPayload(result: TestResult, version: String, buildUrl: String?): JsonValue.Object {
    val jsonObject = jsonObject("success" to result.toBoolean(), "providerApplicationVersion" to version)
    if (buildUrl != null) {
      jsonObject["buildUrl"] = buildUrl
    }

    logger.debug { "Test result = $result" }
    if (result is TestResult.Failed && result.results.isNotEmpty()) {
      val values = result.results
        .groupBy { it["interactionId"] }
        .map { mismatches ->
          val values = mismatches.value
            .filter { !it.containsKey("exception") }
            .map { mismatch ->
              when (mismatch["attribute"]) {
                "body-content-type" -> jsonObject("attribute" to "body", "description" to mismatch["description"])
                else -> jsonObject(
                  mismatch.filterNot { it.key == "interactionId" }.map { it.toPair() }
                )
              }
            }
          val interactionJson = jsonObject("interactionId" to mismatches.key, "success" to false,
            "mismatches" to jsonArray(values)
          )

          val exceptionDetails = mismatches.value.find { it.containsKey("exception") }
          if (exceptionDetails != null) {
            val exception = exceptionDetails["exception"]
            val description = exceptionDetails["description"]
            if (exception is Throwable) {
              if (description != null) {
                interactionJson["exceptions"] = jsonArray(jsonObject("message" to description.toString() + ": " + exception.message,
                  "exceptionClass" to exception.javaClass.name))
              } else {
                interactionJson["exceptions"] = jsonArray(jsonObject("message" to exception.message,
                  "exceptionClass" to exception.javaClass.name))
              }
            } else {
              interactionJson["exceptions"] = jsonArray(jsonObject("message" to exception.toString()))
            }
          }

          interactionJson
        }
      jsonObject["testResults"] = jsonArray(values)
    }
    return jsonObject
  }

  /**
   * Fetches the consumers of the provider that have no associated tag
   */
  @Deprecated(message = "Use the version that takes selectors instead",
    replaceWith = ReplaceWith("fetchConsumersWithSelectors"))
  open fun fetchLatestConsumersWithNoTag(provider: String): List<PactBrokerResult> {
    return try {
      val halClient = newHalClient()
      val consumers = mutableListOf<PactBrokerResult>()
      halClient.navigate(mapOf("provider" to provider), LATEST_PROVIDER_PACTS_WITH_NO_TAG)
        .forAll(PACTS, Consumer { pact ->
          val href = URLDecoder.decode(pact["href"].toString(), UTF8)
          val name = pact["name"].toString()
          if (options.containsKey("authentication")) {
            consumers.add(PactBrokerResult(name, href, pactBrokerUrl, options["authentication"] as List<String>))
          } else {
            consumers.add(PactBrokerResult(name, href, pactBrokerUrl, emptyList()))
          }
        })
      consumers
    } catch (_: NotFoundHalResponse) {
      // This means the provider is not defined in the broker, so fail gracefully.
      emptyList()
    }
  }

  @Deprecated("Use publishProviderTags", replaceWith = ReplaceWith("publishProviderTags"))
  fun publishProviderTag(docAttributes: Map<String, Any?>, name: String, tag: String, version: String) {
    try {
      val halClient = newHalClient()
        .withDocContext(docAttributes)
        .navigate(PROVIDER)
      logPublishingResults(halClient, version, tag, name)
    } catch (e: NotFoundHalResponse) {
      logger.error(e) { "Could not tag provider $name, link was missing" }
    }
  }

  private fun logPublishingResults(halClient: IHalClient, version: String, tag: String, name: String) {
    when (val result = halClient.putJson(PROVIDER_TAG_VERSION, mapOf("version" to version, "tag" to tag), "{}")) {
      is Ok<*> -> logger.debug { "Pushed tag $tag for provider $name and version $version" }
      is Err<Exception> -> logger.error(result.error) { "Failed to push tag $tag for provider $name and version $version" }
    }
  }

  fun publishProviderTags(docAttributes: Map<String, Any?>, name: String, tags: List<String>, version: String) {
    try {
      val halClient = newHalClient()
        .withDocContext(docAttributes)
        .navigate(PROVIDER)
      tags.forEach {
        logPublishingResults(halClient, version, it, name)
      }
    } catch (e: NotFoundHalResponse) {
      logger.error(e) { "Could not tag provider $name, link was missing" }
    }
  }

  open fun canIDeploy(pacticipant: String, pacticipantVersion: String, latest: Latest, to: String?): CanIDeployResult {
    val halClient = newHalClient()
    val path = "/matrix" + buildMatrixQuery(pacticipant, pacticipantVersion, latest, to)
    return retryWith(
      "canIDeploy: Retrying request as there are unknown results",
      config.retryCountWhileUnknown,
      config.retryWhileUnknownInterval,
      { result -> result.ok && result.unknown != null && result.unknown > 0 }
    ) {
      when (val result = halClient.getJson(path, false)) {
        is Ok<JsonValue> -> {
          val summary = result.value["summary"].asObject()
          CanIDeployResult(Json.toBoolean(summary["deployable"]), "", Json.toString(summary["reason"]),
            Json.toInteger(summary["unknown"]))
        }
        is Err<Exception> -> {
          logger.error(result.error) { "Pact broker matrix query failed: ${result.error.message}" }
          CanIDeployResult(false, result.error.message.toString(), "")
        }
      }
    }
  }

  private fun buildMatrixQuery(pacticipant: String, pacticipantVersion: String, latest: Latest, to: String?): String {
    val escaper = urlPathSegmentEscaper()
    var base = "?q[][pacticipant]=${escaper.escape(pacticipant)}&latestby=cvp"
    base += when (latest) {
      is Latest.UseLatest -> if (latest.latest) {
        "&q[][latest]=true"
      } else {
        "&q[][version]=${escaper.escape(pacticipantVersion)}"
      }
      is Latest.UseLatestTag -> "q[][tag]=${escaper.escape(latest.latestTag)}"
    }
    base += if (to.isNotEmpty()) {
      "&latest=true&tag=${escaper.escape(to)}"
    } else {
      "&latest=true"
    }
    return base
  }

  open fun createVersionTag(
    pacticipant: String,
    pacticipantVersion: String,
    tag: String
  ) =
      uploadTags(
          newHalClient(),
          pacticipant,
          pacticipantVersion,
          listOf(tag)
      )

  companion object : KLogging() {
    const val LATEST_PROVIDER_PACTS_WITH_NO_TAG = "pb:latest-untagged-pact-version"
    const val LATEST_PROVIDER_PACTS = "pb:latest-provider-pacts"
    const val LATEST_PROVIDER_PACTS_WITH_TAG = "pb:latest-provider-pacts-with-tag"
    const val PROVIDER_PACTS_FOR_VERIFICATION = "pb:provider-pacts-for-verification"
    const val BETA_PROVIDER_PACTS_FOR_VERIFICATION = "beta:provider-pacts-for-verification"
    const val PROVIDER = "pb:provider"
    const val PROVIDER_TAG_VERSION = "pb:version-tag"
    const val PACTS = "pb:pacts"
    const val UTF8 = "UTF-8"

    fun uploadTags(
      halClient: IHalClient,
      consumerName: String,
      version: String,
      tags: List<String>
    ): Result<String?, Exception> {
      halClient.navigate()
      var result = Ok("") as Result<String?, Exception>
      tags.forEach {
        result = uploadTag(halClient, consumerName, version, it)
      }
      return result
    }

    private fun uploadTag(halClient: IHalClient, consumerName: String, version: String, it: String): Result<String?, Exception> {
      val result = halClient.putJson("pb:pacticipant-version-tag", mapOf(
          "pacticipant" to consumerName,
          "version" to version,
          "tag" to it
      ), "{}")

      if (result is Err<Exception>) {
        logger.error(result.error) { "Failed to push tag $it for consumer $consumerName and version $version" }
      }

      return result
    }

    fun <T> retryWith(
      message: String,
      count: Int,
      interval: Int,
      predicate: (T) -> Boolean,
      function: () -> T
    ): T {
      var counter = 0
      var result = function()
      while (counter < count && predicate(result)) {
        counter += 1
        logger.info { "$message [$counter/$count]" }
        Thread.sleep((interval * 1000).toLong())
        result = function()
      }
      return result
    }
  }
}
