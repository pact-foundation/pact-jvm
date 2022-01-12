package au.com.dius.pact.core.pactbroker

import au.com.dius.pact.core.pactbroker.util.HttpClientUtils.buildUrl
import au.com.dius.pact.core.pactbroker.util.HttpClientUtils.isJsonResponse
import au.com.dius.pact.core.support.Auth
import au.com.dius.pact.core.support.HttpClient
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.Json.fromJson
import au.com.dius.pact.core.support.handleWith
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import au.com.dius.pact.core.support.json.get
import au.com.dius.pact.core.support.jsonObject
import au.com.dius.pact.core.support.unwrap
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.google.common.net.UrlEscapers
import mu.KLogging
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.classic.methods.HttpPut
import org.apache.hc.client5.http.impl.auth.BasicAuthCache
import org.apache.hc.client5.http.impl.auth.BasicScheme
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.HttpMessage
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.io.entity.StringEntity
import java.net.URI
import java.util.function.BiFunction
import java.util.function.Consumer

/**
 * Interface to a HAL Client
 */
interface IHalClient {
  /**
   * Navigates to the Root
   */
  @Throws(InvalidNavigationRequest::class)
  fun navigate(): IHalClient

  /**
   * Navigates the URL associated with the given link using the current HAL document
   * @param options Map of key-value pairs to use for parsing templated links
   * @param link Link name to navigate
   */
  fun navigate(options: Map<String, Any> = mapOf(), link: String): IHalClient

  /**
   * Navigates the URL associated with the given link using the current HAL document
   * @param link Link name to navigate
   */
  fun navigate(link: String): IHalClient

  /**
   * Returns the HREF of the named link from the current HAL document
   */
  fun linkUrl(name: String): String?

  /**
   * Calls the closure with a Map of attributes for all links associated with the link name
   * @param linkName Name of the link to loop over
   * @param closure Closure to invoke with the link attributes
   */
  fun forAll(linkName: String, closure: Consumer<Map<String, Any?>>)

  /**
   * Upload the JSON document to the provided URL, using a POST request
   * @param url Url to upload the document to
   * @param body JSON contents for the body
   * @return Returns a Success result object with a boolean value to indicate if the request was successful or not. Any
   * exception will be wrapped in a Failure
   */
  fun postJson(url: String, body: String): Result<Boolean, Exception>

  /**
   * Upload the JSON document to the provided URL, using a POST request
   * @param url Url to upload the document to
   * @param body JSON contents for the body
   * @param handler Response handler
   * @return Returns a Success result object with the boolean value returned from the handler closure. Any
   * exception will be wrapped in a Failure
   */
  fun postJson(
    url: String,
    body: String,
    handler: ((status: Int, response: ClassicHttpResponse) -> Boolean)?
  ): Result<Boolean, Exception>

  /**
   * Fetches the HAL document from the provided path
   * @param path The path to the HAL document. If it is a relative path, it is relative to the base URL
   * @param encodePath If the path should be encoded to make a valid URL
   */
  fun fetch(path: String, encodePath: Boolean): Result<JsonValue.Object, Exception>

  /**
   * Fetches the HAL document from the provided path
   * @param path The path to the HAL document. If it is a relative path, it is relative to the base URL
   */
  fun fetch(path: String): Result<JsonValue.Object, Exception>

  /**
   * Sets the starting context from a previous broker interaction (Pact document)
   */
  fun withDocContext(docAttributes: Map<String, Any?>): IHalClient

  /**
   * Sets the starting context from a previous broker interaction (Pact document)
   */
  fun withDocContext(docAttributes: JsonValue.Object): IHalClient

  /**
   * Upload a JSON document to the current path link, using a PUT request
   */
  fun putJson(link: String, options: Map<String, Any>, json: String): Result<String?, Exception>

  /**
   * Upload a JSON document to the current path link, using a POST request
   */
  fun postJson(link: String, options: Map<String, Any>, json: String): Result<JsonValue.Object, Exception>

  /**
   * Get JSON from the provided path
   */
  fun getJson(path: String): Result<JsonValue, Exception>

  /**
   * Get JSON from the provided path
   * @param path Path to fetch the JSON document from
   * @param encodePath If the path should be encoded
   */
  fun getJson(path: String, encodePath: Boolean): Result<JsonValue, Exception>
}

/**
 * HAL client for navigating the HAL links
 */
open class HalClient @JvmOverloads constructor(
  val baseUrl: String,
  @Deprecated("Move use of options to PactBrokerClientConfig")
  var options: Map<String, Any> = mapOf(),
  val config: PactBrokerClientConfig
) : IHalClient {

  var httpClient: CloseableHttpClient? = null
  var httpContext: HttpClientContext? = null
  var pathInfo: JsonValue.Object? = null
  var lastUrl: String? = null
  var defaultHeaders: MutableMap<String, String> = mutableMapOf()
  private var maxPublishRetries = 5
  private var publishRetryInterval = 3000

  init {
    if (options.containsKey("halClient")) {
      val halClient = options["halClient"] as Map<String, Any>
      maxPublishRetries = halClient.getOrDefault("maxPublishRetries", this.maxPublishRetries) as Int
      publishRetryInterval = halClient.getOrDefault("publishRetryInterval", this.publishRetryInterval) as Int
    }
  }

  fun <Method : HttpMessage> initialiseRequest(method: Method): Method {
    defaultHeaders.forEach { (key, value) -> method.addHeader(key, value) }
    return method
  }

  override fun postJson(url: String, body: String) = postJson(url, body, null)

  override fun postJson(
    url: String,
    body: String,
    handler: ((status: Int, response: ClassicHttpResponse) -> Boolean)?
  ): Result<Boolean, Exception> {
    logger.debug { "Posting JSON to $url\n$body" }
    val client = setupHttpClient()

    return handleWith {
      val httpPost = initialiseRequest(HttpPost(url))
      httpPost.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString())
      httpPost.entity = StringEntity(body, ContentType.APPLICATION_JSON)

      client.execute(httpPost, httpContext) {
        logger.debug { "Got response ${it.code} ${it.reasonPhrase}" }
        logger.debug { "Response body: ${it.entity?.content?.reader()?.readText()}" }
        if (handler != null) {
          handler(it.code, it)
        } else if (it.code >= 300) {
          logger.error { "PUT JSON request failed with status ${it.code} ${it.reasonPhrase}" }
          Err(RequestFailedException(it.code, if (it.entity != null) EntityUtils.toString(it.entity) else null))
        } else {
          true
        }
      }
    }
  }

  open fun setupHttpClient(): CloseableHttpClient {
    if (httpClient == null) {
      if (options.containsKey("authentication") && options["authentication"] !is Auth &&
        options["authentication"] !is List<*>) {
        logger.warn { "Authentication options needs to be either an instance of Auth or a list of values, ignoring." }
      }
      val uri = URI(baseUrl)
      val result = HttpClient.newHttpClient(options["authentication"], uri, this.maxPublishRetries,
        this.publishRetryInterval, config.insecureTLS)
      httpClient = result.first

      if (System.getProperty(PREEMPTIVE_AUTHENTICATION) == "true") {
        val targetHost = HttpHost(uri.scheme, uri.host, uri.port)
        logger.warn { "Using preemptive basic authentication with the pact broker at $targetHost" }
        val authCache = BasicAuthCache()
        val basicAuth = BasicScheme()
        authCache.put(targetHost, basicAuth)
        httpContext = HttpClientContext.create()
        httpContext!!.credentialsProvider = result.second
        httpContext!!.authCache = authCache
      }
    }

    return httpClient!!
  }

  @Throws(InvalidNavigationRequest::class)
  override fun navigate(): IHalClient {
    when (val result = fetch(ROOT)) {
      is Ok<JsonValue.Object> -> pathInfo = result.value
      is Err<Exception> -> {
        logger.warn { "Failed to fetch the root HAL document" }
        throw InvalidNavigationRequest("Failed to fetch the root HAL document", result.error)
      }
    }
    return this
  }

  override fun navigate(options: Map<String, Any>, link: String): IHalClient {
    pathInfo = pathInfo ?: fetch(ROOT).unwrap()
    pathInfo = fetchLink(link, options)
    return this
  }

  override fun navigate(link: String) = navigate(mapOf(), link)

  override fun fetch(path: String) = fetch(path, true)

  override fun fetch(path: String, encodePath: Boolean): Result<JsonValue.Object, Exception> {
    lastUrl = path
    logger.debug { "Fetching: $path" }
    return when (val result = getJson(path, encodePath)) {
      is Ok -> when (result.value) {
        is JsonValue.Object -> Ok(result.value)
        else -> Err(RuntimeException("Expected a JSON document, but found a ${result.value}"))
      }
      is Err -> result
    } as Result<JsonValue.Object, Exception>
  }

  override fun withDocContext(docAttributes: Map<String, Any?>): IHalClient {
    val links = JsonValue.Object()
    links[LINKS] = jsonObject(docAttributes.entries.map {
      it.key to when (it.value) {
        is Map<*, *> -> jsonObject((it.value as Map<*, *>).entries.map { entry ->
          if (entry.key == "href") {
            entry.key.toString() to entry.value.toString()
          } else {
            entry.key.toString() to entry.value
          }
        })
        else -> JsonValue.Null
      }
    })
    pathInfo = links
    return this
  }

  override fun withDocContext(docAttributes: JsonValue.Object): IHalClient {
    pathInfo = docAttributes
    return this
  }

  override fun getJson(path: String) = getJson(path, true)

  override fun getJson(path: String, encodePath: Boolean): Result<JsonValue, Exception> {
    setupHttpClient()
    return handleWith {
      val httpGet = initialiseRequest(HttpGet(buildUrl(baseUrl, path, encodePath)))
      httpGet.addHeader("Content-Type", "application/json")
      httpGet.addHeader("Accept", "application/hal+json, application/json")

      httpClient!!.execute(httpGet, httpContext) {
        handleHalResponse(it, path)
      }
    }
  }

  private fun handleHalResponse(response: ClassicHttpResponse, path: String): Result<JsonValue, Exception> {
    return if (response.code < 300) {
      val contentType = ContentType.parseLenient(response.entity.contentType)
      if (isJsonResponse(contentType)) {
        Ok(JsonParser.parseString(EntityUtils.toString(response.entity)))
      } else {
        Err(InvalidHalResponse("Expected a HAL+JSON response from the pact broker, but got '$contentType'"))
      }
    } else {
      when (response.code) {
        404 -> Err(NotFoundHalResponse("No HAL document found at path '$path'"))
        else -> {
          val body = if (response.entity != null) EntityUtils.toString(response.entity) else null
          Err(RequestFailedException(response.code, body,
            "Request to path '$path' failed with response ${response.code}"))
        }
      }
    }
  }

  private fun fetchLink(link: String, options: Map<String, Any>): JsonValue.Object {
    val href = hrefForLink(link, options)
    return this.fetch(href, false).unwrap()
  }

  private fun hrefForLink(link: String, options: Map<String, Any>): String {
    if (pathInfo[LINKS].isNull) {
      throw InvalidHalResponse("Expected a HAL+JSON response from the pact broker, but got " +
        "a response with no '_links'. URL: '$baseUrl', LINK: '$link'")
    }

    val links = pathInfo[LINKS]
    if (links is JsonValue.Object) {
      if (!links.has(link)) {
        throw InvalidHalResponse("Link '$link' was not found in the response, only the following links where " +
          "found: ${links.entries.keys}. URL: '$baseUrl', LINK: '$link'")
      }
      val linkData = links[link]
      if (linkData is JsonValue.Array) {
        if (options.containsKey("name")) {
          val linkByName = linkData.find { it is JsonValue.Object && it["name"] == options["name"] }
          return if (linkByName is JsonValue.Object && linkByName["templated"].isBoolean) {
            parseLinkUrl(linkByName["href"].toString(), options)
          } else if (linkByName is JsonValue.Object) {
            Json.toString(linkByName["href"])
          } else {
            throw InvalidNavigationRequest("Link '$link' does not have an entry with name '${options["name"]}'. " +
              "URL: '$baseUrl', LINK: '$link'")
          }
        } else {
          throw InvalidNavigationRequest("Link '$link' has multiple entries. You need to filter by the link name. " +
            "URL: '$baseUrl', LINK: '$link'")
        }
      } else if (linkData is JsonValue.Object) {
        return if (linkData.has("templated") && linkData["templated"].isBoolean) {
          parseLinkUrl(Json.toString(linkData["href"]), options)
        } else {
          Json.toString(linkData["href"])
        }
      } else {
        throw InvalidHalResponse("Expected link in map form in the response, but " +
          "found: $linkData. URL: '$baseUrl', LINK: '$link'")
      }
    } else {
      throw InvalidHalResponse("Expected a map of links in the response, but " +
        "found: $links. URL: '$baseUrl', LINK: '$link'")
    }
  }

  fun parseLinkUrl(href: String, options: Map<String, Any>): String {
    var result = ""
    var match = URL_TEMPLATE_REGEX.find(href)
    var index = 0
    while (match != null) {
      val start = match.range.first - 1
      if (start >= index) {
        result += href.substring(index..start)
      }
      index = match.range.last + 1
      val (key) = match.destructured
      result += encodePathParameter(options, key, match.value)

      match = URL_TEMPLATE_REGEX.find(href, index)
    }

    if (index < href.length) {
      result += href.substring(index)
    }
    return result
  }

  private fun encodePathParameter(options: Map<String, Any>, key: String, value: String): String? {
    return UrlEscapers.urlPathSegmentEscaper().escape(options[key]?.toString() ?: value)
  }

  fun initPathInfo() {
    pathInfo = pathInfo ?: fetch(ROOT).unwrap()
  }

  fun handleFailure(resp: ClassicHttpResponse, body: String?, closure: BiFunction<String, String, Any?>): Any? {
    if (resp.entity.contentType != null) {
      val contentType = ContentType.parseLenient(resp.entity.contentType)
      if (isJsonResponse(contentType)) {
        var error = ""
        if (body.isNotEmpty()) {
          val jsonBody = JsonParser.parseString(body!!)
          if (jsonBody.has("errors")) {
            val errors = jsonBody["errors"]
            if (errors is JsonValue.Array) {
              error = " - " + errors.values.joinToString(", ") { Json.toString(it) }
            } else if (errors is JsonValue.Object) {
              error = " - " + errors.entries.entries.joinToString(", ") { entry ->
                if (entry.value is JsonValue.Array) {
                  "${entry.key}: ${(entry.value as JsonValue.Array).values.joinToString(", ") { Json.toString(it) }}"
                } else {
                  "${entry.key}: ${entry.value.asString()}"
                }
              }
            }
          }
        }
        return closure.apply("FAILED", "${resp.code} ${resp.reasonPhrase}$error")
      } else {
        return closure.apply("FAILED", "${resp.code} ${resp.reasonPhrase} - $body")
      }
    } else {
      return closure.apply("FAILED", "${resp.code} ${resp.reasonPhrase} - $body")
    }
  }

  override fun linkUrl(name: String): String? {
    if (pathInfo != null && pathInfo!!.has(LINKS)) {
      val links = pathInfo!![LINKS]
      if (links is JsonValue.Object && links.has(name)) {
        val linkData = links[name]
        if (linkData is JsonValue.Object && linkData.has("href")) {
          return fromJson(linkData["href"]).toString()
        }
      }
    }

    return null
  }

  override fun forAll(linkName: String, closure: Consumer<Map<String, Any?>>) {
    initPathInfo()
    val links = pathInfo!![LINKS]
    if (links is JsonValue.Object && links.has(linkName)) {
      val matchingLink = links[linkName]
      if (matchingLink is JsonValue.Array) {
        matchingLink.values.forEach { closure.accept(asMap(it.asObject())) }
      } else {
        closure.accept(asMap(matchingLink.asObject()))
      }
    }
  }

  override fun putJson(link: String, options: Map<String, Any>, json: String): Result<String?, Exception> {
    val href = hrefForLink(link, options)
    val httpPut = initialiseRequest(HttpPut(buildUrl(baseUrl, href, false)))
    httpPut.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString())
    httpPut.entity = StringEntity(json, ContentType.APPLICATION_JSON)

    return handleWith {
      httpClient!!.execute(httpPut, httpContext) {
        when {
          it.code < 300 -> if (it.entity != null) EntityUtils.toString(it.entity) else null
          else -> {
            logger.error { "PUT JSON request failed with status ${it.code} ${it.reasonPhrase}" }
            Err(RequestFailedException(it.code, if (it.entity != null) EntityUtils.toString(it.entity) else null))
          }
        }
      }
    }
  }

  override fun postJson(link: String, options: Map<String, Any>, json: String): Result<JsonValue.Object, Exception> {
    val href = hrefForLink(link, options)
    val http = initialiseRequest(HttpPost(buildUrl(baseUrl, href, false)))
    http.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString())
    http.addHeader("Accept", "application/hal+json, application/json")
    http.entity = StringEntity(json, ContentType.APPLICATION_JSON)

    return handleWith {
      httpClient!!.execute(http, httpContext) {
        handleHalResponse(it, href)
      }
    }
  }

  companion object : KLogging() {
    const val ROOT = "/"
    const val LINKS = "_links"
    const val PREEMPTIVE_AUTHENTICATION = "pact.pactbroker.httpclient.usePreemptiveAuthentication"

    val URL_TEMPLATE_REGEX = Regex("\\{(\\w+)}")

    @JvmStatic
    fun asMap(jsonObject: JsonValue.Object?) = jsonObject?.entries?.entries?.associate {
        entry -> entry.key to fromJson(entry.value)
    } ?: emptyMap()
  }
}
