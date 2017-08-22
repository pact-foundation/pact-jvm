package au.com.dius.pact.provider.broker

import au.com.dius.pact.pactbroker.IHalClient
import au.com.dius.pact.pactbroker.NotFoundHalResponse
import au.com.dius.pact.pactbroker.PactBrokerClientBase
import au.com.dius.pact.pactbroker.PactBrokerConsumer
import au.com.dius.pact.pactbroker.PactResponse
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import org.apache.commons.lang3.StringUtils

/**
 * Client for the pact broker service
 */
@Canonical
class PactBrokerClient extends PactBrokerClientBase {

  private static final String LATEST_PROVIDER_PACTS = 'pb:latest-provider-pacts'
  private static final String LATEST_PROVIDER_PACTS_WITH_TAG = 'pb:latest-provider-pacts-with-tag'
  private static final String PACTS = 'pacts'
  private static final String UTF8 = 'UTF-8'

  PactBrokerClient(String pactBrokerUrl, Map<String, ?> options) {
    super(pactBrokerUrl, options)
  }

  PactBrokerClient(String pactBrokerUrl) {
    super(pactBrokerUrl, [:])
  }

  @SuppressWarnings('EmptyCatchBlock')
  List<PactBrokerConsumer> fetchConsumers(String provider) {
    List consumers = []

    try {
      IHalClient halClient = newHalClient()
      halClient.navigate(LATEST_PROVIDER_PACTS, provider: provider).forAll(PACTS) { pact ->
        def href = URLDecoder.decode(pact.href, UTF8)
        if (options.authentication) {
          consumers << new PactBrokerConsumer(pact.name, href, pactBrokerUrl, options.authentication)
        } else {
          consumers << new PactBrokerConsumer(pact.name, href, pactBrokerUrl, [])
        }
      }
    }
    catch (NotFoundHalResponse e) {
      // This means the provider is not defined in the broker, so fail gracefully.
    }

    consumers
  }

  @SuppressWarnings('EmptyCatchBlock')
  List<PactBrokerConsumer> fetchConsumersWithTag(String provider, String tag) {
    List consumers = []

    try {
      IHalClient halClient = newHalClient()
      halClient.navigate(LATEST_PROVIDER_PACTS_WITH_TAG, provider: provider, tag: tag).forAll(PACTS) { pact ->
        def href = URLDecoder.decode(pact.href, UTF8)
        if (options.authentication) {
          consumers << new PactBrokerConsumer(pact.name, href, pactBrokerUrl, options.authentication)
        } else {
          consumers << new PactBrokerConsumer(pact.name, href, pactBrokerUrl, [])
        }
      }
    }
    catch (NotFoundHalResponse e) {
      // This means the provider is not defined in the broker, so fail gracefully.
    }

    consumers
  }

  protected IHalClient newHalClient() {
    new HalClient(pactBrokerUrl, options)
  }

  def uploadPactFile(File pactFile, String version, List<String> tags = []) {
    def pactText = pactFile.text
    def pact = new JsonSlurper().parseText(pactText)
    IHalClient halClient = newHalClient()
    def uploadPath = "/pacts/provider/${pact.provider.name}/consumer/${pact.consumer.name}/version/$version"
    halClient.uploadJson(uploadPath, pactText) { result, status ->
      if (result == 'OK') {
        if (tags) {
          uploadTags(halClient, pact.consumer.name, version, tags)
        }
        status
      } else {
        "FAILED! $status"
      }
    }
  }

  def uploadTags(HalClient halClient, String provider, String version, List<String> tags) {
    tags.each {
      halClient.uploadJson("/pacticipants/$provider/versions/$version/tags/$it", '')
    }
  }

  String getUrlForProvider(String providerName, String tag) {
    IHalClient halClient = newHalClient()
    if (StringUtils.isEmpty(tag)) {
      halClient.navigate(LATEST_PROVIDER_PACTS, provider: providerName)
    } else {
      halClient.navigate(LATEST_PROVIDER_PACTS_WITH_TAG, provider: providerName, tag: tag)
    }
    halClient.linkUrl(PACTS)
  }

  PactResponse fetchPact(String url) {
    HalClient halClient = newHalClient()
    def halDoc = halClient.fetch(url)
    new PactResponse(halDoc, halDoc.'_links')
  }
}
