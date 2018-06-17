package au.com.dius.pact.provider

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.com.github.kittinunf.result.Result
import groovy.lang.GroovyObjectSupport
import mu.KotlinLogging
import java.util.function.Function

private val logger = KotlinLogging.logger {}

@JvmOverloads
fun <I> reportVerificationResults(pact: Pact<I>, result: Boolean, version: String, client: PactBrokerClient? = null)
  where I: Interaction {
  val source = pact.source
  when (source) {
    is BrokerUrlSource -> {
      val brokerClient = client ?: PactBrokerClient(source.pactBrokerUrl, source.options)
      publishResult(brokerClient, source, result, version, pact)
    }
    else -> logger.info { "Skipping publishing verification results for source $source" }
  }
}

private fun <I> publishResult(brokerClient: PactBrokerClient, source: BrokerUrlSource, result: Boolean, version: String, pact: Pact<I>) where I : Interaction {
  val publishResult = brokerClient.publishVerificationResults(source.attributes, result, version)
  if (publishResult is Result.Failure) {
    logger.warn { "Failed to publish verification results - ${publishResult.error.localizedMessage}" }
    logger.debug(publishResult.error) {}
  } else {
    logger.info { "Published verification result of '$result' for consumer '${pact.consumer}'" }
  }
}

open class ProviderVerifierBase : GroovyObjectSupport() {

  var projectHasProperty = Function<String, Boolean> { name -> !System.getProperty(name).isNullOrEmpty() }
  var projectGetProperty = Function<String, String?> { name -> System.getProperty(name) }

  /**
   * This will return true unless the pact.verifier.publishResults property has the value of "true"
   */
  open fun publishingResultsDisabled(): Boolean {
    return !projectHasProperty.apply(PACT_VERIFIER_PUBLISHRESUTS) ||
      projectGetProperty.apply(PACT_VERIFIER_PUBLISHRESUTS)?.toLowerCase() != "true"
  }

  companion object {
    const val PACT_VERIFIER_PUBLISHRESUTS = "pact.verifier.publishResults"
  }
}
