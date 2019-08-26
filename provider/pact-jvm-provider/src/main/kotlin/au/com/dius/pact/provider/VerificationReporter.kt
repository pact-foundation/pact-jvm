package au.com.dius.pact.provider

import au.com.dius.pact.com.github.michaelbull.result.Err
import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.TestResult
import mu.KLogging

interface VerificationReporter {
  @Deprecated(message = "Use the method that takes a test result")
  fun reportResults(pact: Pact<out Interaction>, result: Boolean, version: String, client: PactBrokerClient? = null)
  fun reportResults(pact: Pact<out Interaction>, result: TestResult, version: String, client: PactBrokerClient? = null)

  /**
   * This must return true unless the pact.verifier.publishResults property has the value of "true"
   */
  fun publishingResultsDisabled(): Boolean
}

object DefaultVerificationReporter : VerificationReporter, KLogging() {

  override fun reportResults(pact: Pact<out Interaction>, result: Boolean, version: String, client: PactBrokerClient?) {
    reportResults(pact, TestResult.fromBoolean(result), version, client)
  }

  override fun reportResults(pact: Pact<out Interaction>, result: TestResult, version: String, client: PactBrokerClient?) {
    when (val source = pact.source) {
      is BrokerUrlSource -> {
        val brokerClient = client ?: PactBrokerClient(source.pactBrokerUrl, source.options)
        publishResult(brokerClient, source, result, version, pact)
      }
      else -> logger.info { "Skipping publishing verification results for source $source" }
    }
  }

  private fun <I> publishResult(
    brokerClient: PactBrokerClient,
    source: BrokerUrlSource,
    result: TestResult,
    version: String,
    pact: Pact<out I>
  ) where I : Interaction {
    val publishResult = brokerClient.publishVerificationResults(source.attributes, result, version)
    if (publishResult is Err) {
      logger.error { "Failed to publish verification results - ${publishResult.error.localizedMessage}" }
      logger.debug(publishResult.error) {}
    } else {
      logger.info { "Published verification result of '$result' for consumer '${pact.consumer}'" }
    }
  }

  override fun publishingResultsDisabled() =
    System.getProperty(ProviderVerifier.PACT_VERIFIER_PUBLISH_RESULTS)?.toLowerCase() != "true"
}
