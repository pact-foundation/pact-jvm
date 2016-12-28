package au.com.dius.pact.provider.broker

import au.com.dius.pact.provider.ConsumerInfo
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import groovyx.net.http.HTTPBuilder

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.PUT

/**
 * Client for the pact broker service
 */
@Canonical
class PactBrokerClient {

  String pactBrokerUrl
  Map options = [:]

  @SuppressWarnings('EmptyCatchBlock')
  List fetchConsumers(String provider) {
    List consumers = []

    try {
      HalClient halClient = newHalClient()
      halClient.navigate('pb:latest-provider-pacts', provider: provider).pacts { pact ->
        consumers << new ConsumerInfo(pact.name, new URL(pact.href))
        if (options.authentication) {
          consumers.last().pactFileAuthentication = options.authentication
        }
      }
    }
    catch (NotFoundHalResponse e) {
      // This means the provider is not defined in the broker, so fail gracefully.
    }

    consumers
  }

  @SuppressWarnings('EmptyCatchBlock')
  List fetchConsumersWithTag(String provider, String tag) {
    List consumers = []

    try {
      HalClient halClient = newHalClient()
      halClient.navigate('pb:latest-provider-pacts-with-tag', provider: provider, tag: tag).pacts { pact ->
        consumers << new ConsumerInfo(pact.name, new URL(pact.href))
        if (options.authentication) {
          consumers.last().pactFileAuthentication = options.authentication
        }
      }
    }
    catch (NotFoundHalResponse e) {
      // This means the provider is not defined in the broker, so fail gracefully.
    }

    consumers
  }

  private newHalClient() {
    new HalClient(pactBrokerUrl, options)
  }

  def uploadPactFile(File pactFile, String version) {
    def pact = new JsonSlurper().parse(pactFile)
    def http = new HTTPBuilder(pactBrokerUrl)
    http.parser.'application/hal+json' = http.parser.'application/json'
    http.request(PUT) {
      uri.path = "/pacts/provider/${pact.provider.name}/consumer/${pact.consumer.name}/version/$version"
      body = pactFile.text
      requestContentType = JSON

      response.success = { resp -> resp.statusLine as String }

      response.failure = { resp, json ->
        def error = json?.errors?.join(', ') ?: 'Unknown error'
        "FAILED! ${resp.statusLine.statusCode} ${resp.statusLine.reasonPhrase} - ${error}"
      }

      response.'409' = { resp, body ->
        "FAILED! ${resp.statusLine.statusCode} ${resp.statusLine.reasonPhrase} - ${body.readLine()}"
      }
    }
  }
}
