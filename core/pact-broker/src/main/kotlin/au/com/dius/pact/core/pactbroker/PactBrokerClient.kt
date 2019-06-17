package au.com.dius.pact.core.pactbroker

import au.com.dius.pact.com.github.michaelbull.result.Err
import au.com.dius.pact.com.github.michaelbull.result.Result
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.string
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.toJson
import com.google.common.net.UrlEscapers.urlPathSegmentEscaper
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.dmfs.rfc3986.encoding.Precoded
import java.io.File
import java.net.URLDecoder
import java.util.function.BiFunction
import java.util.function.Consumer

/**
 * Wraps the response for a Pact from the broker with the link data associated with the Pact document.
 */
data class PactResponse(val pactFile: JsonObject, val links: Map<String, Map<String, Any>>)

sealed class TestResult {
  object Ok: TestResult() {
    override fun toBoolean() = true

    override fun merge(result: TestResult) = when (result) {
      is Ok -> this
      is Failed -> result
    }
  }

  data class Failed(var results: List<Any> = emptyList()): TestResult() {
    override fun toBoolean() = false

    override fun merge(result: TestResult) = when (result) {
      is Ok -> this
      is Failed -> Failed(results + result.results)
    }
  }

  abstract fun toBoolean(): Boolean
  abstract fun merge(result: TestResult): TestResult

  companion object {
    fun fromBoolean(result: Boolean) = if (result) Ok else Failed()
  }
}

/**
 * Client for the pact broker service
 */
open class PactBrokerClient(val pactBrokerUrl: String, val options: Map<String, Any>) {

  constructor(pactBrokerUrl: String) : this(pactBrokerUrl, mapOf())

  /**
   * Fetches all consumers for the given provider
   */
  open fun fetchConsumers(provider: String): List<PactBrokerConsumer> {
    return try {
      val halClient = newHalClient()
      val consumers = mutableListOf<PactBrokerConsumer>()
      halClient.navigate(mapOf("provider" to provider), LATEST_PROVIDER_PACTS).forAll(PACTS, Consumer { pact ->
        val href = Precoded(pact["href"].toString()).decoded().toString()
        val name = pact["name"].toString()
        if (options.containsKey("authentication")) {
          consumers.add(PactBrokerConsumer(name, href, pactBrokerUrl, options["authentication"] as List<String>))
        } else {
          consumers.add(PactBrokerConsumer(name, href, pactBrokerUrl))
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
  open fun fetchConsumersWithTag(provider: String, tag: String): List<PactBrokerConsumer> {
    return try {
      val halClient = newHalClient()
      val consumers = mutableListOf<PactBrokerConsumer>()
      halClient.navigate(mapOf("provider" to provider, "tag" to tag), LATEST_PROVIDER_PACTS_WITH_TAG)
        .forAll(PACTS, Consumer { pact ->
        val href = Precoded(pact["href"].toString()).decoded().toString()
        val name = pact["name"].toString()
        if (options.containsKey("authentication")) {
          consumers.add(PactBrokerConsumer(name, href, pactBrokerUrl, options["authentication"] as List<String>, tag))
        } else {
          consumers.add(PactBrokerConsumer(name, href, pactBrokerUrl, emptyList(), tag))
        }
      })
      consumers
    } catch (e: NotFoundHalResponse) {
      // This means the provider is not defined in the broker, so fail gracefully.
      emptyList()
    }
  }

  /**
   * Uploads the given pact file to the broker, and optionally applies any tags
   */
  @JvmOverloads
  open fun uploadPactFile(pactFile: File, unescapedVersion: String, tags: List<String> = emptyList()): Any? {
    val pactText = pactFile.readText()
    val pact = JsonParser().parse(pactText)
    val halClient = newHalClient()
    val providerName = urlPathSegmentEscaper().escape(pact["provider"]["name"].string)
    val consumerName = urlPathSegmentEscaper().escape(pact["consumer"]["name"].string)
    val version = urlPathSegmentEscaper().escape(unescapedVersion)
    val uploadPath = "/pacts/provider/$providerName/consumer/$consumerName/version/$version"
    if (tags.isNotEmpty()) {
      uploadTags(halClient, consumerName, version, tags)
    }
    return halClient.uploadJson(uploadPath, pactText, BiFunction { result, status ->
      if (result == "OK") {
        status
      } else {
        "FAILED! $status"
      }
    }, false)
  }

  open fun getUrlForProvider(providerName: String, tag: String): String? {
    val halClient = newHalClient()
    if (tag.isEmpty() || tag == "latest") {
      halClient.navigate(mapOf("provider" to providerName), LATEST_PROVIDER_PACTS)
    } else {
      halClient.navigate(mapOf("provider" to providerName, "tag" to tag), LATEST_PROVIDER_PACTS_WITH_TAG)
    }
    return halClient.linkUrl(PACTS)
  }

  open fun fetchPact(url: String): PactResponse {
    val halDoc = newHalClient().fetch(url).obj
    return PactResponse(halDoc, HalClient.asMap(halDoc["_links"].obj) as Map<String, Map<String, Any>>)
  }

  open fun newHalClient(): IHalClient = HalClient(pactBrokerUrl, options)

  @Deprecated(message = "Use the version that takes a test result",
    replaceWith = ReplaceWith("publishVerificationResults"))
  open fun publishVerificationResults(
    docAttributes: Map<String, Map<String, Any>>,
    result: Boolean,
    version: String,
    buildUrl: String? = null
  ): Result<Boolean, Exception>
    = publishVerificationResults(docAttributes, TestResult.fromBoolean(result), version, buildUrl)

  /**
   * Publishes the result to the "pb:publish-verification-results" link in the document attributes.
   */
  @JvmOverloads
  open fun publishVerificationResults(
    docAttributes: Map<String, Map<String, Any>>,
    result: TestResult,
    version: String,
    buildUrl: String? = null
  ): Result<Boolean, Exception> {
    val halClient = newHalClient()
    val publishLink = docAttributes.mapKeys { it.key.toLowerCase() } ["pb:publish-verification-results"] // ktlint-disable curly-spacing
    return if (publishLink != null) {
      val jsonObject = jsonObject("success" to result.toBoolean(), "providerApplicationVersion" to version)
      if (buildUrl != null) {
        jsonObject.add("buildUrl", buildUrl.toJson())
      }
      val lowercaseMap = publishLink.mapKeys { it.key.toLowerCase() }
      if (lowercaseMap.containsKey("href")) {
        halClient.postJson(lowercaseMap["href"].toString(), jsonObject.toString())
      } else {
        Err(RuntimeException("Unable to publish verification results as there is no " +
          "pb:publish-verification-results link"))
      }
    } else {
      Err(RuntimeException("Unable to publish verification results as there is no " +
        "pb:publish-verification-results link"))
    }
  }

  /**
   * Fetches the consumers of the provider that have no associated tag
   */
  open fun fetchLatestConsumersWithNoTag(provider: String): List<PactBrokerConsumer> {
    return try {
      val halClient = newHalClient()
      val consumers = mutableListOf<PactBrokerConsumer>()
      halClient.navigate(mapOf("provider" to provider), LATEST_PROVIDER_PACTS_WITH_NO_TAG)
        .forAll(PACTS, Consumer { pact ->
          val href = URLDecoder.decode(pact["href"].toString(), UTF8)
          val name = pact["name"].toString()
          if (options.containsKey("authentication")) {
            consumers.add(PactBrokerConsumer(name, href, pactBrokerUrl, options["authentication"] as List<String>))
          } else {
            consumers.add(PactBrokerConsumer(name, href, pactBrokerUrl, emptyList()))
          }
        })
      consumers
    } catch (_: NotFoundHalResponse) {
      // This means the provider is not defined in the broker, so fail gracefully.
      emptyList()
    }
  }

  companion object {
    const val LATEST_PROVIDER_PACTS_WITH_NO_TAG = "pb:latest-untagged-pact-version"
    const val LATEST_PROVIDER_PACTS = "pb:latest-provider-pacts"
    const val LATEST_PROVIDER_PACTS_WITH_TAG = "pb:latest-provider-pacts-with-tag"
    const val PACTS = "pacts"
    const val UTF8 = "UTF-8"

    fun uploadTags(halClient: IHalClient, consumerName: String, version: String, tags: List<String>) {
      tags.forEach {
        val tag = urlPathSegmentEscaper().escape(it)
        halClient.uploadJson("/pacticipants/$consumerName/versions/$version/tags/$tag", "",
          BiFunction { _, _ -> null }, false)
      }
    }
  }
}
