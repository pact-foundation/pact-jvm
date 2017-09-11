package au.com.dius.pact.pactbroker

import au.com.dius.pact.provider.broker.com.github.kittinunf.result.Result
import mu.KLogging
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
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

  companion object : KLogging()
}
