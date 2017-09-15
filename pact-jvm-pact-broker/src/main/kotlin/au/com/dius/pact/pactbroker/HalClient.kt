package au.com.dius.pact.pactbroker

import au.com.dius.pact.provider.broker.com.github.kittinunf.result.Result
import com.github.salomonbrys.kotson.fromJson
import com.google.common.net.UrlEscapers
import com.google.gson.Gson
import mu.KLogging
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.net.URI
import java.util.function.BiFunction
import java.util.function.Consumer

/**
 * Interface to a HAL Client
 */
interface IHalClient {
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
  fun linkUrl(name: String): String

  /**
   * Calls the closure with a Map of attributes for all links associated with the link name
   * @param linkName Name of the link to loop over
   * @param closure Closure to invoke with the link attributes
   */
  fun forAll(linkName: String, closure: Consumer<Map<String, Any>>)

  /**
   * Upload the JSON document to the provided path, using a PUT request
   * @param path Path to upload the document
   * @param bodyJson JSON contents for the body
   */
  fun uploadJson(path: String, bodyJson: String): Any?

  /**
   * Upload the JSON document to the provided path, using a PUT request
   * @param path Path to upload the document
   * @param bodyJson JSON contents for the body
   * @param closure Closure that will be invoked with details about the response. The result from the closure will be
   * returned.
   */
  fun uploadJson(path: String, bodyJson: String, closure: BiFunction<String, String, Any?>): Any?

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
  fun postJson(url: String, body: String, handler: ((status: Int, response: CloseableHttpResponse) -> Boolean)?): Result<Boolean, Exception>
}

/**
 * HAL client base class
 */
abstract class HalClientBase @JvmOverloads constructor(val baseUrl: String,
                                                       var options: Map<String, Any> = mapOf()) : IHalClient {

  var httpClient: CloseableHttpClient? = null
  var pathInfo: Map<String, Any>? = null
  var lastUrl: String? = null

  override fun postJson(url: String, body: String) = postJson(url, body, null)

  override fun postJson(url: String, body: String,
                        handler: ((status: Int, response: CloseableHttpResponse) -> Boolean)?): Result<Boolean, Exception> {
    val client = setupHttpClient()

    return Result.of {
      val httpPost = HttpPost(url)
      httpPost.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString())
      httpPost.entity = StringEntity(body, ContentType.APPLICATION_JSON)

      client.execute(httpPost).use {
        if (handler != null) {
          handler(it.statusLine.statusCode, it)
        } else {
          it.statusLine.statusCode < 300
        }
      }
    }
  }

  open fun setupHttpClient(): CloseableHttpClient {
    if (httpClient == null) {
      val builder = HttpClients.custom().useSystemProperties()
      if (options["authentication"] is List<*>) {
        val authentication = options["authentication"] as List<*>
        val scheme = authentication.first().toString().toLowerCase()
        when (scheme) {
          "basic" -> {
            if (authentication.size > 2) {
              val credsProvider = BasicCredentialsProvider()
              val uri = URI(baseUrl)
              credsProvider.setCredentials(AuthScope(uri.host, uri.port),
                UsernamePasswordCredentials(authentication[1].toString(), authentication[2].toString()))
              builder.setDefaultCredentialsProvider(credsProvider)
            } else {
              logger.warn { "Basic authentication requires a username and password, ignoring." }
            }
          }
          else -> logger.warn { "Hal client Only supports basic authentication, got '$scheme', ignoring." }
        }
      } else if (options.containsKey("authentication")) {
        logger.warn { "Authentication options needs to be a list of values, ignoring." }
      }

      httpClient = builder.build()
    }

    return httpClient!!
  }

  override fun navigate(options: Map<String, Any>, link: String): IHalClient {
    pathInfo = pathInfo ?: fetch(ROOT)
    pathInfo = fetchLink(link, options)
    return this
  }

  override fun navigate(link: String) = navigate(mapOf(), link)

  fun fetch(path: String): Map<String, Any> {
    lastUrl = path
    logger.debug { "Fetching: $path" }
    val response = getJson(path)
    when (response) {
      is Result.Success -> return response.value as Map<String, Any>
      is Result.Failure -> throw response.error
    }
  }

  private fun getJson(path: String): Result<Map<*, *>, Exception> {
    setupHttpClient()
    return Result.of {
      val pathUrl = URI(path)
      val url = if (pathUrl.isAbsolute) pathUrl else URIBuilder(baseUrl).setPath(path).build()
      val httpGet = HttpGet(url)
      httpGet.addHeader("Content-Type", "application/json")
      httpGet.addHeader("Accept", "application/hal+json, application/json")

      val response = httpClient!!.execute(httpGet)
      if (response.statusLine.statusCode < 300) {
        val contentType = ContentType.getOrDefault(response.entity)
        if (isJsonResponse(contentType)) {
          return@of Gson().fromJson<Map<*, *>>(EntityUtils.toString(response.entity))
        } else {
          throw InvalidHalResponse("Expected a HAL+JSON response from the pact broker, but got '$contentType'")
        }
      } else {
        when (response.statusLine.statusCode) {
          404 -> throw NotFoundHalResponse("No HAL document found at path '$path'")
          else -> throw RequestFailedException("Request to path '$path' failed with response '${response.statusLine}'")
        }
      }
    }
  }

  private fun isJsonResponse(contentType: ContentType) = contentType.mimeType == "application/json" ||
    contentType.mimeType == "application/hal+json"

  private fun fetchLink(link: String, options: Map<String, Any>): Map<String, Any> {
    if (pathInfo == null || pathInfo!!["_links"] == null) {
      throw InvalidHalResponse("Expected a HAL+JSON response from the pact broker, but got " +
        "a response with no '_links'. URL: '$baseUrl', LINK: '$link'")
    }

    val links = pathInfo!!["_links"]
    if (links is Map<*, *>) {
      val linkData = links[link]
      if (linkData == null) {
        throw InvalidHalResponse("Link '$link' was not found in the response, only the following links where " +
          "found: ${links.keys}. URL: '$baseUrl', LINK: '$link'")
      }

      if (linkData is List<*>) {
        if (options.containsKey("name")) {
          val linkByName = linkData.find { it is Map<*, *> && it["name"] == options["name"] }
          return if (linkByName is Map<*, *> && linkByName["templated"] as Boolean) {
            this.fetch(parseLinkUrl(linkByName["href"].toString(), options))
          } else if (linkByName is Map<*, *>) {
            this.fetch(linkByName["href"].toString())
          } else {
            throw InvalidNavigationRequest("Link '$link' does not have an entry with name '${options["name"]}'. " +
              "URL: '$baseUrl', LINK: '$link'")
          }
        } else {
          throw InvalidNavigationRequest ("Link '$link' has multiple entries. You need to filter by the link name. " +
            "URL: '$baseUrl', LINK: '$link'")
        }
      } else if (linkData is Map<*, *>) {
        return if (linkData.containsKey("templated") && linkData["templated"] as Boolean) {
          fetch(parseLinkUrl(linkData["href"].toString(), options))
        } else {
          fetch(linkData["href"].toString())
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
      val start = match.range.start - 1
      if (start >= index) {
        result += href.substring(index..start)
      }
      index = match.range.endInclusive + 1
      val (key) = match.destructured
      result += encodePathParameter(options, key, match.value)

      match = URL_TEMPLATE_REGEX.find(href, index)
    }

    if (index < href.length) {
      result += href.substring(index)
    }
    return result
  }

  private fun encodePathParameter(options: Map<String, Any>, key: String, value: String) =
    UrlEscapers.urlPathSegmentEscaper().escape(options[key]?.toString() ?: value)

  fun initPathInfo() {
    pathInfo = pathInfo ?: fetch(ROOT)
  }

  companion object : KLogging() {
    const val ROOT = "/"
    val URL_TEMPLATE_REGEX = Regex("\\{(\\w+)\\}")
  }
}
