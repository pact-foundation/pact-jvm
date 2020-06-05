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
import org.dmfs.rfc3986.encoding.Precoded
import java.io.File
import java.net.URLDecoder
import java.util.function.Consumer

/**
 * Wraps the response for a Pact from the broker with the link data associated with the Pact document.
 */
data class PactResponse(val pactFile: JsonValue.Object, val links: Map<String, Any?>)

sealed class TestResult {
  object Ok : TestResult() {
    override fun toBoolean() = true

    override fun merge(result: TestResult) = when (result) {
      is Ok -> this
      is Failed -> result
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

data class CanIDeployResult(val ok: Boolean, val message: String, val reason: String)

/**
 * Consumer version selector. See https://docs.pact.io/pact_broker/advanced_topics/selectors
 */
data class ConsumerVersionSelector(val tag: String, val latest: Boolean = true) {
  fun toJson() = JsonValue.Object("tag" to Json.toJson(tag), "latest" to Json.toJson(latest))
}

interface IPactBrokerClient {
  /**
   * Fetches all consumers for the given provider and selectors
   */
  fun fetchConsumersWithSelectors(
    providerName: String,
    selectors: List<ConsumerVersionSelector>,
    providerTags: List<String> = emptyList(),
    enablePending: Boolean = false
  ): Result<List<PactBrokerResult>, Exception>

  fun getUrlForProvider(providerName: String, tag: String): String?

  val options: Map<String, Any>
}

/**
 * Client for the pact broker service
 */
open class PactBrokerClient(val pactBrokerUrl: String, override val options: Map<String, Any>) : IPactBrokerClient {

  constructor(pactBrokerUrl: String) : this(pactBrokerUrl, mapOf())

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
        val href = Precoded(pact["href"].toString()).decoded().toString()
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
        val href = Precoded(pact["href"].toString()).decoded().toString()
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
    enablePending: Boolean
  ): Result<List<PactBrokerResult>, Exception> {
    val navigateResult = handleWith<IHalClient> { newHalClient().navigate() }
    val halClient = when (navigateResult) {
      is Err<Exception> -> return Err(navigateResult.error)
      is Ok<IHalClient> -> navigateResult.value
    }
    val pactsForVerification = when {
      halClient.linkUrl(PROVIDER_PACTS_FOR_VERIFICATION) != null -> PROVIDER_PACTS_FOR_VERIFICATION
      halClient.linkUrl(BETA_PROVIDER_PACTS_FOR_VERIFICATION) != null -> BETA_PROVIDER_PACTS_FOR_VERIFICATION
      else -> null
    }
    if (pactsForVerification != null) {
      val body = JsonValue.Object(
        "consumerVersionSelectors" to jsonArray(selectors.map { it.toJson() })
      )
      if (enablePending) {
        body["providerVersionTags"] = jsonArray(providerTags)
        body["includePendingStatus"] = true
      }

      return handleWith {
        halClient.postJson(pactsForVerification, mapOf("provider" to providerName), body.serialise()).map { result ->
          result["_embedded"]["pacts"].asArray().map { pactJson ->
            val selfLink = pactJson["_links"]["self"]
            val href = Precoded(Json.toString(selfLink["href"])).decoded().toString()
            val name = Json.toString(selfLink["name"])
            val properties = pactJson["verificationProperties"]
            val notices = properties["notices"].asArray()
              .map { VerificationNotice.fromJson(it.asObject()) }
            var pending = false
            if (properties is JsonValue.Object && properties.has("pending") && properties["pending"].isBoolean) {
              pending = properties["pending"].asBoolean()
            }
            if (options.containsKey("authentication")) {
              PactBrokerResult(name, href, pactBrokerUrl, options["authentication"] as List<String>, notices, pending)
            } else {
              PactBrokerResult(name, href, pactBrokerUrl, emptyList(), notices, pending)
            }
          }
        }
      }
    } else {
      return handleWith {
        if (selectors.isEmpty()) {
          fetchConsumers(providerName)
        } else {
          fetchConsumersWithTag(providerName, selectors.first().tag)
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
    unescapedVersion: String,
    tags: List<String> = emptyList()
  ): Result<Boolean, Exception> {
    val pactText = pactFile.readText()
    val pact = JsonParser.parseString(pactText)
    val halClient = newHalClient()
    val providerName = urlPathSegmentEscaper().escape(pact["provider"]["name"].asString())
    val consumerName = urlPathSegmentEscaper().escape(pact["consumer"]["name"].asString())
    val version = urlPathSegmentEscaper().escape(unescapedVersion)
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
            .flatMap { mismatch ->
              when (mismatch["type"]) {
                "body" -> {
                  when (val bodyMismatches = mismatch["comparison"]) {
                    is Map<*, *> -> bodyMismatches.entries.filter { it.key != "diff" }.flatMap { entry ->
                      val values = entry.value as List<Map<String, Any>>
                      values.map {
                        jsonObject("attribute" to "body", "identifier" to entry.key, "description" to it["mismatch"],
                          "diff" to it["diff"])
                      }
                    }
                    else -> listOf(jsonObject("attribute" to "body", "description" to bodyMismatches.toString()))
                  }
                }
                "status" -> listOf(jsonObject("attribute" to "status", "description" to mismatch["description"]))
                "header" -> {
                  listOf(jsonObject(mismatch.filter { it.key != "interactionId" }
                    .map {
                      if (it.key == "type") {
                        "attribute" to it.value
                      } else {
                        it.toPair()
                      }
                    }))
                }
                "metadata" -> {
                  listOf(jsonObject(mismatch.filter { it.key != "interactionId" }
                    .flatMap {
                      when (it.key) {
                        "type" -> listOf("attribute" to it.value)
                        else -> listOf("identifier" to it.key, "description" to it.value)
                      }
                    }))
                }
                else -> listOf(jsonObject(
                  mismatch.filterNot { it.key == "interactionId" || it.key == "type" }.entries.map {
                    it.toPair()
                  }
                ))
              }
            }
          val interactionJson = jsonObject("interactionId" to mismatches.key, "success" to false,
            "mismatches" to jsonArray(values)
          )

          val exceptionDetails = mismatches.value.find { it.containsKey("exception") }
          if (exceptionDetails != null) {
            val exception = exceptionDetails["exception"]
            if (exception is Throwable) {
              interactionJson["exceptions"] = jsonArray(jsonObject("message" to exception.message,
                "exceptionClass" to exception.javaClass.name))
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

  fun publishProviderTag(docAttributes: Map<String, Any?>, name: String, tag: String, version: String) {
    return try {
      val halClient = newHalClient()
        .withDocContext(docAttributes)
        .navigate(PROVIDER)
      when (val result = halClient.putJson(PROVIDER_TAG_VERSION, mapOf("version" to version, "tag" to tag), "{}")) {
        is Ok<*> -> logger.debug { "Pushed tag $tag for provider $name and version $version" }
        is Err<Exception> -> logger.error(result.error) { "Failed to push tag $tag for provider $name and version $version" }
      }
    } catch (e: NotFoundHalResponse) {
      logger.error(e) { "Could not tag provider $name, link was missing" }
    }
  }

  open fun canIDeploy(pacticipant: String, pacticipantVersion: String, latest: Latest, to: String?): CanIDeployResult {
    val halClient = newHalClient()
    val result = halClient.getJson("/matrix" + buildMatrixQuery(pacticipant, pacticipantVersion, latest, to),
      false)
    return when (result) {
      is Ok<JsonValue> -> {
        val summary = result.value["summary"].asObject()
        CanIDeployResult(Json.toBoolean(summary["deployable"]), "", Json.toString(summary["reason"]))
      }
      is Err<Exception> -> {
        logger.error(result.error) { "Pact broker matrix query failed: ${result.error.message}" }
        CanIDeployResult(false, result.error.message.toString(), "")
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

    fun uploadTags(halClient: IHalClient, consumerName: String, version: String, tags: List<String>) {
      halClient.navigate()
      tags.forEach {
        val result = halClient.putJson("pb:pacticipant-version-tag", mapOf(
          "pacticipant" to consumerName,
          "version" to version,
          "tag" to it
        ), "{}")
        if (result is Err<Exception>) {
          logger.error(result.error) { "Failed to push tag $it for consumer $consumerName and version $version" }
        }
      }
    }
  }
}
