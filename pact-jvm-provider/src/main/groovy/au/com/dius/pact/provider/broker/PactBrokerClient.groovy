package au.com.dius.pact.provider.broker

import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.PactVerifierException
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

  List fetchConsumers(String provider) {
    List consumers = []

    HalClient halClient = newHalClient()
    try {
      halClient.navigate('pb:latest-provider-pacts', provider: provider).pacts { pact ->
        consumers << new ConsumerInfo(pact.name, new URL(pact.href))
        if (options.authentication) {
          consumers.last().pactFileAuthentication = options.authentication
        }
      }
    } catch (e) {
      throw new PactVerifierException("Failed to load pacts from pact broker at '$pactBrokerUrl'", e)
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
    http.request(PUT, JSON) {
      uri.path = "/pacts/provider/${pact.provider.name}/consumer/${pact.consumer.name}/version/$version"
      requestContentType = JSON
      body = pactFile.text

      response.success = { resp -> resp.statusLine }

      response.failure = { resp, json ->
        def error = json?.errors?.join(', ') ?: 'Unknown error'
        "FAILED! ${resp.statusLine.statusCode} ${resp.statusLine.reasonPhrase} - ${error}"
      }
    }
  }
}
