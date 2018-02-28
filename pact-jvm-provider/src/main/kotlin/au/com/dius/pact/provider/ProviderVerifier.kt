package au.com.dius.pact.provider

import au.com.dius.pact.model.BrokerUrlSource
import au.com.dius.pact.model.Pact
import au.com.dius.pact.provider.broker.PactBrokerClient
import au.com.dius.pact.provider.broker.com.github.kittinunf.result.Result
import groovy.lang.GroovyObjectSupport
import mu.KotlinLogging
import java.util.function.Function

private val logger = KotlinLogging.logger {}

@JvmOverloads
fun reportVerificationResults(pact: Pact, result: Boolean, version: String, client: PactBrokerClient? = null) {
  val source = pact.source
  when (source) {
    is BrokerUrlSource -> {
      val brokerClient = client ?: PactBrokerClient(source.pactBrokerUrl, source.options)
      val publishResult = brokerClient.publishVerificationResults(source.attributes, result, version)
      if (publishResult is Result.Failure) {
        logger.warn { "Failed to publish verification results - ${publishResult.error.localizedMessage}" }
        logger.debug(publishResult.error) {}
      } else {
        logger.info { "Published verification result of '$result' for consumer '${pact.consumer}'" }
      }
    }
  }
}

open class ProviderVerifierBase : GroovyObjectSupport() {

  var projectHasProperty = Function<String, Boolean> { name -> !System.getProperty(name).isNullOrEmpty() }
  var projectGetProperty = Function<String, String?> { name -> System.getProperty(name) }

  /**
   * This will return true if the pact.verifier.publishResults property is present and has the value of "false"
   */
  open fun publishingResultsDisabled(): Boolean {
    return projectHasProperty.apply(PACT_VERIFIER_PUBLISHRESUTS) &&
      projectGetProperty.apply(PACT_VERIFIER_PUBLISHRESUTS)?.toLowerCase() == "false"
  }

  companion object {
    const val PACT_VERIFIER_PUBLISHRESUTS = "pact.verifier.publishResults"
  }
}
