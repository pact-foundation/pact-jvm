package au.com.dius.pact.model

import au.com.dius.pact.com.github.michaelbull.result.Err
import au.com.dius.pact.com.github.michaelbull.result.Ok
import au.com.dius.pact.com.github.michaelbull.result.Result
import au.com.dius.pact.provider.broker.PactBrokerClient
import au.com.dius.pact.util.HttpClientUtils
import au.com.dius.pact.util.HttpClientUtils.isJsonResponse
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import groovy.json.JsonSlurper
import mu.KotlinLogging
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.net.URI
import java.net.URL

private val logger = KotlinLogging.logger {}

private val ACCEPT_JSON = mutableMapOf("requestProperties" to mutableMapOf("Accept" to "application/json"))

data class InvalidHttpResponseException(override val message: String) : RuntimeException(message)

fun loadPactFromUrl(source: UrlPactSource, options: Map<String, Any>, http: CloseableHttpClient?): Pair<Any, PactSource> {
  return when (source) {
    is BrokerUrlSource -> {
      val brokerClient = PactBrokerClient(source.pactBrokerUrl, options)
      val pactResponse = brokerClient.fetchPact(source.url)
      pactResponse.pactFile to source.copy(attributes = pactResponse.links, options = options)
    }
    else -> if (options.containsKey("authentication")) {
      val jsonResource = fetchJsonResource(http!!, source)
      when (jsonResource) {
        is Ok -> jsonResource.value
        is Err -> throw jsonResource.error
      }
    } else {
      JsonSlurper().parse(URL(source.url), ACCEPT_JSON) to source
    }
  }
}

fun fetchJsonResource(http: CloseableHttpClient, source: UrlPactSource):
  Result<Pair<JsonElement, UrlPactSource>, Exception> {
  return Result.of {
    val httpGet = HttpGet(HttpClientUtils.buildUrl("", source.url, true))
    httpGet.addHeader("Content-Type", "application/json")
    httpGet.addHeader("Accept", "application/hal+json, application/json")

    val response = http.execute(httpGet)
    if (response.statusLine.statusCode < 300) {
      val contentType = ContentType.getOrDefault(response.entity)
      if (isJsonResponse(contentType)) {
        return@of JsonParser().parse(EntityUtils.toString(response.entity)) to source
      } else {
        throw InvalidHttpResponseException("Expected a JSON response, but got '$contentType'")
      }
    } else {
      when (response.statusLine.statusCode) {
        404 -> throw InvalidHttpResponseException("No JSON document found at source '$source'")
        else -> throw InvalidHttpResponseException("Request to source '$source' failed with response " +
          "'${response.statusLine}'")
      }
    }
  }
}

fun newHttpClient(baseUrl: String, options: Map<String, Any>): CloseableHttpClient {
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
      else -> logger.warn { "Only supports basic authentication, got '$scheme', ignoring." }
    }
  } else if (options.containsKey("authentication")) {
    logger.warn { "Authentication options needs to be a list of values, got '${options["authentication"]}', ignoring." }
  }

  return builder.build()
}
