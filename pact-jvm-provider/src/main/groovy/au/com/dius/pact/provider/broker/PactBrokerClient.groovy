package au.com.dius.pact.provider.broker

import au.com.dius.pact.provider.ConsumerInfo
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.apache.commons.lang3.StringUtils

/**
 * Client for the pact broker service
 */
@Canonical
class PactBrokerClient {

  private static final String LATEST_PROVIDER_PACTS = 'pb:latest-provider-pacts'
  private static final String LATEST_PROVIDER_PACTS_WITH_TAG = 'pb:latest-provider-pacts-with-tag'
  private static final String PACTS = 'pacts'

  def pactBrokerUrl
  Map options = [:]

  @SuppressWarnings('EmptyCatchBlock')
  List<ConsumerInfo> fetchConsumers(String provider) {
    List consumers = []

    try {
      HalClient halClient = newHalClient()
      halClient.navigate(LATEST_PROVIDER_PACTS, provider: provider).pacts { pact ->
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
  List<ConsumerInfo> fetchConsumersWithTag(String provider, String tag) {
    List consumers = []

    try {
      HalClient halClient = newHalClient()
      halClient.navigate(LATEST_PROVIDER_PACTS_WITH_TAG, provider: provider, tag: tag).pacts { pact ->
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
    def pactText = pactFile.text
    def pact = new JsonSlurper().parseText(pactText)
    HalClient halClient = newHalClient()
    halClient.uploadJson("/pacts/provider/${pact.provider.name}/consumer/${pact.consumer.name}/version/$version",
      pactText) { result, status ->
      if (result == 'OK') {
        status
      } else {
        "FAILED! $status"
      }
    }
  }

  String getUrlForProvider(String providerName, String tag) {
    HalClient halClient = newHalClient()
    if (StringUtils.isEmpty(tag)) {
      halClient.navigate(LATEST_PROVIDER_PACTS, provider: providerName)
    } else {
      halClient.navigate(LATEST_PROVIDER_PACTS_WITH_TAG, provider: providerName, tag: tag)
    }
    halClient.linkUrl(PACTS)
  }

  @SuppressWarnings(['UnusedMethodParameter', 'UnusedVariable'])
  String publishVerificationResults(String providerName, String consumer, boolean result, String version,
                                  String buildUrl) {
    HalClient halClient = newHalClient()
    def publishLink = halClient.navigate(LATEST_PROVIDER_PACTS, provider: providerName)
      .navigate(PACTS, name: consumer).linkUrl('pb:publish-verification-results')
    def verificationResult = [success: result, providerApplicationVersion: version]
    if (StringUtils.isNotEmpty(buildUrl)) {
      verificationResult.buildUrl = buildUrl
    }
    halClient.post(publishLink, verificationResult)
  }
}
