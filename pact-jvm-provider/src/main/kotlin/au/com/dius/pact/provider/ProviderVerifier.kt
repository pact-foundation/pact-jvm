package au.com.dius.pact.provider

import au.com.dius.pact.com.github.michaelbull.result.Err
import au.com.dius.pact.model.BrokerUrlSource
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.provider.broker.PactBrokerClient
import au.com.dius.pact.provider.reporters.VerifierReporter
import groovy.lang.GroovyObjectSupport
import mu.KotlinLogging
import java.util.function.BiConsumer
import java.util.function.Function

private val logger = KotlinLogging.logger {}

interface VerificationReporter {
  fun <I> reportResults(pact: Pact<I>, result: Boolean, version: String, client: PactBrokerClient? = null)
    where I: Interaction
}

@JvmOverloads
@Deprecated("Use the VerificationReporter instead of this function")
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

object DefaultVerificationReporter : VerificationReporter {
  override fun <I> reportResults(pact: Pact<I>, result: Boolean, version: String, client: PactBrokerClient?)
    where I: Interaction = reportVerificationResults(pact, result, version, client)
}

private fun <I> publishResult(brokerClient: PactBrokerClient, source: BrokerUrlSource, result: Boolean, version: String, pact: Pact<I>) where I : Interaction {
  val publishResult = brokerClient.publishVerificationResults(source.attributes, result, version)
  if (publishResult is Err) {
    logger.warn { "Failed to publish verification results - ${publishResult.error.localizedMessage}" }
    logger.debug(publishResult.error) {}
  } else {
    logger.info { "Published verification result of '$result' for consumer '${pact.consumer}'" }
  }
}

/**
 * Interface to the provider verifier
 */
interface IProviderVerifier {
  /**
   * List of the all reporters to report the results of the verification to
   */
  var reporters: List<VerifierReporter>

  /**
   * Callback to determine if something is a build specific task
   */
  var checkBuildSpecificTask: Function<Any, Boolean>

  /**
   * Consumer SAM to execute the build specific task
   */
  var executeBuildSpecificTask: BiConsumer<Any, ProviderState>

  /**
   * Callback to determine is the project has a particular property
   */
  var projectHasProperty: Function<String, Boolean>

  /**
   * Reports the state of the interaction to all the registered reporters
   */
  fun reportStateForInteraction(state: String, provider: IProviderInfo, consumer: IConsumerInfo, isSetup: Boolean)
}

abstract class ProviderVerifierBase : GroovyObjectSupport(), IProviderVerifier {

  override var projectHasProperty = Function<String, Boolean> { name -> !System.getProperty(name).isNullOrEmpty() }
  var projectGetProperty = Function<String, String?> { name -> System.getProperty(name) }

  /**
   * This will return true unless the pact.verifier.publishResults property has the value of "true"
   */
  open fun publishingResultsDisabled(): Boolean {
    return !projectHasProperty.apply(PACT_VERIFIER_PUBLISH_RESULTS) ||
      projectGetProperty.apply(PACT_VERIFIER_PUBLISH_RESULTS)?.toLowerCase() != "true"
  }

  companion object {
    const val PACT_VERIFIER_PUBLISH_RESULTS = "pact.verifier.publishResults"
    const val PACT_FILTER_CONSUMERS = "pact.filter.consumers"
    const val PACT_FILTER_DESCRIPTION = "pact.filter.description"
    const val PACT_FILTER_PROVIDERSTATE = "pact.filter.providerState"
    const val PACT_SHOW_STACKTRACE = "pact.showStacktrace"
    const val PACT_SHOW_FULLDIFF = "pact.showFullDiff"
  }
}
