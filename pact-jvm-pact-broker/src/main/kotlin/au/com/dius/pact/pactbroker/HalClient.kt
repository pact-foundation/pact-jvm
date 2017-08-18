package au.com.dius.pact.pactbroker

import com.github.kittinunf.result.Result
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

interface IHalClient {
  fun navigate(options: Map<String, Any> = mapOf(), link: String): IHalClient
  fun navigate(link: String): IHalClient
  fun linkUrl(name: String): String
  fun forAll(linkName: String, closure: Consumer<Map<String, Any>>)

  fun uploadJson(path: String, bodyJson: String): Any?
  fun uploadJson(path: String, bodyJson: String, closure: BiFunction<String, String, Any?>): Any?

  fun postJson(url: String, body: String): Result<Boolean, Exception>
  fun postJson(url: String, body: String, handler: ((status: Int, response: CloseableHttpResponse) -> Boolean)? = null): Result<Boolean, Exception>
}

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
