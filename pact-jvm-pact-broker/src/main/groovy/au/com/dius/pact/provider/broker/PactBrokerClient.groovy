package au.com.dius.pact.provider.broker

import au.com.dius.pact.pactbroker.IHalClient
import au.com.dius.pact.pactbroker.NotFoundHalResponse
import au.com.dius.pact.pactbroker.PactBrokerClientBase
import au.com.dius.pact.pactbroker.PactBrokerConsumer
import au.com.dius.pact.pactbroker.PactResponse
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper

/**
 * Client for the pact broker service
 */
@Canonical
@Slf4j
class PactBrokerClient extends PactBrokerClientBase {

  private static final String LATEST_PROVIDER_PACTS = 'pb:latest-provider-pacts'
  private static final String LATEST_PROVIDER_PACTS_WITH_TAG = 'pb:latest-provider-pacts-with-tag'

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
        def href = new URI(pact.href).getPath()
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
    catch (URISyntaxException e) {
      log.error("URL to the pact is invalid", e)
      throw new RuntimeException("URL to the pact is invalid", e)
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

  def uploadPactFile(File pactFile, String unescapedVersion, List<String> tags = []) {
    def pactText = pactFile.text
    def pact = new JsonSlurper().parseText(pactText)
    IHalClient halClient = newHalClient()
    def providerName = urlPathSegmentEscaper().escape(pact.provider.name)
    def consumerName = urlPathSegmentEscaper().escape(pact.consumer.name)
    def version =  urlPathSegmentEscaper().escape(unescapedVersion)
    def uploadPath = "/pacts/provider/$providerName/consumer/$consumerName/version/$version"
    halClient.uploadJson(uploadPath, pactText, { result, status ->
      if (result == 'OK') {
        if (tags) {
          uploadTags(halClient, consumerName, version, tags)
        }
        status
      } else {
        "FAILED! $status"
      }
    }, false)
  }

  static uploadTags(IHalClient halClient, String consumerName, String version, List<String> tags) {
    tags.each {
      def tag = urlPathSegmentEscaper().escape(it)
      halClient.uploadJson("/pacticipants/$consumerName/versions/$version/tags/$tag", '',
        { p1, p2 -> null }, false)
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
    def halDoc = newHalClient().fetch(url)
    new PactResponse(HalClient.asMap(halDoc), HalClient.asMap(halDoc['_links']))
  }
}
