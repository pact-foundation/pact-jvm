package au.com.dius.pact.provider

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.TestResult
import com.github.michaelbull.result.Err
import mu.KLogging

/**
 * Interface to the reporter that published the verification results
 */
interface VerificationReporter {
  /**
   * Publish the results to the pact broker. If the tag is given, then the provider will be tagged with that first.
   */
  @Deprecated("Use version that takes a list of provider tags")
  fun reportResults(
    pact: Pact<out Interaction>,
    result: TestResult,
    version: String,
    client: PactBrokerClient? = null,
    tag: String? = null
  )

  /**
   * Publish the results to the pact broker. If the tags are given, then the provider will be tagged with those first.
   */
  fun reportResults(
    pact: Pact<out Interaction>,
    result: TestResult,
    version: String,
    client: PactBrokerClient? = null,
    tags: List<String> = emptyList()
  )

  /**
   * This must return true unless the pact.verifier.publishResults property has the value of "true"
   */
  fun publishingResultsDisabled(): Boolean
}

/**
 * Default implementation of a verification reporter
 */
object DefaultVerificationReporter : VerificationReporter, KLogging() {

  override fun reportResults(
    pact: Pact<out Interaction>,
    result: TestResult,
    version: String,
    client: PactBrokerClient?,
    tag: String?
  ) {
    if (tag.isNullOrEmpty()) {
      reportResults(pact, result, version, client, emptyList())
    } else {
      reportResults(pact, result, version, client, listOf(tag))
    }
  }

  override fun reportResults(
    pact: Pact<out Interaction>,
    result: TestResult,
    version: String,
    client: PactBrokerClient?,
    tags: List<String>
  ) {
    when (val source = pact.source) {
      is BrokerUrlSource -> {
        val brokerClient = client ?: PactBrokerClient(source.pactBrokerUrl, source.options)
        publishResult(brokerClient, source, result, version, pact, tags)
      }
      else -> logger.info { "Skipping publishing verification results for source $source" }
    }
  }

  private fun <I : Interaction> publishResult(
    brokerClient: PactBrokerClient,
    source: BrokerUrlSource,
    result: TestResult,
    version: String,
    pact: Pact<out I>,
    tags: List<String>
  ) {
    if (tags.isNotEmpty()) {
      brokerClient.publishProviderTags(source.attributes, pact.provider.name, tags, version)
    }
    val publishResult = brokerClient.publishVerificationResults(source.attributes, result, version)
    if (publishResult is Err) {
      logger.error { "Failed to publish verification results - ${publishResult.error}" }
    } else {
      logger.info { "Published verification result of '$result' for consumer '${pact.consumer}'" }
    }
  }

  override fun publishingResultsDisabled(): Boolean {
    var property = System.getProperty(ProviderVerifier.PACT_VERIFIER_PUBLISH_RESULTS)
    if (property.isNullOrEmpty()) {
      property = System.getenv(ProviderVerifier.PACT_VERIFIER_PUBLISH_RESULTS)
    }
    return property?.toLowerCase() != "true"
  }
}
