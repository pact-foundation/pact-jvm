package au.com.dius.pact.model

import au.com.dius.pact.provider.broker.PactBrokerClient
import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import mu.KotlinLogging
import java.net.URL

private val logger = KotlinLogging.logger {}

private val ACCEPT_JSON = mutableMapOf("requestProperties" to mutableMapOf("Accept" to "application/json"))

data class InvalidHttpResponseException(override val message: String) : RuntimeException(message)

fun loadPactFromUrl(source: UrlPactSource, options: Map<String, Any>, http: RESTClient): Pair<Any, PactSource> {
  when (source) {
    is BrokerUrlSource -> {
      val brokerClient = PactBrokerClient(source.pactBrokerUrl, options)
      val pactResponse = brokerClient.fetchPact(source.url)
      return pactResponse.pactFile to source.copy(attributes = pactResponse.links, options = options)
    }
    else -> {
      if (options.containsKey("authentication")) {
        val auth = options["authentication"]
        if (auth is List<*>) {
          setupHttpAuthentication(auth, http)
        } else {
          logger.warn { "Ignoring invalid authentication values '$auth' - it should be a list" }
        }
        val response = http.get(mutableMapOf("headers" to mutableMapOf("Accept" to "application/json")))
        if (response is HttpResponseDecorator) {
          return response.data!! to source
        } else {
          throw InvalidHttpResponseException("Received an invalid response from the HTTP client: $response")
        }
      } else {
        return JsonSlurper().parse(URL(source.url), ACCEPT_JSON) to source
      }
    }
  }
}

private fun setupHttpAuthentication(auth: List<*>, http: RESTClient) {
  when (auth.first().toString().toLowerCase()) {
    "basic" -> if (auth.size > 2) {
      http.auth.basic(auth[1].toString(), auth[2].toString())
    } else {
      logger.warn { "Basic authentication requires a username and password, ignoring." }
    }
    else -> logger.warn { "Unrecognised authentication scheme: $auth" }
  }
}
