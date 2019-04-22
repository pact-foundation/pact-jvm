package au.com.dius.pact.provider

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.provider.ProviderVerifierBase.Companion.PACT_VERIFIER_PUBLISH_RESULTS
import mu.KLogging
import org.apache.commons.lang3.builder.HashCodeBuilder

interface TestResultAccumulator {
  fun updateTestResult(pact: Pact<Interaction>, interaction: Interaction, testExecutionResult: Boolean)
}

object DefaultTestResultAccumulator : TestResultAccumulator, KLogging() {

  private val testResults: MutableMap<Int, MutableMap<Int, Boolean>> = mutableMapOf()
  var verificationReporter: VerificationReporter = DefaultVerificationReporter

  override fun updateTestResult(
    pact: Pact<Interaction>,
    interaction: Interaction,
    testExecutionResult: Boolean
  ) {
    logger.debug { "Received test result '$testExecutionResult' for Pact ${pact.provider.name}-${pact.consumer.name} " +
      "and ${interaction.description}" }
    val pactHash = calculatePactHash(pact)
    val interactionResults = testResults.getOrPut(pactHash) { mutableMapOf() }
    val interactionHash = calculateInteractionHash(interaction)
    interactionResults[interactionHash] = testExecutionResult
    if (allInteractionsVerified(pact, interactionResults)) {
      logger.debug { "All interactions for Pact ${pact.provider.name}-${pact.consumer.name} are verified" }
      if (verificationReporter.publishingResultsDisabled()) {
        logger.warn { "Skipping publishing of verification results as it has been disabled " +
          "($PACT_VERIFIER_PUBLISH_RESULTS is not 'true')" }
      } else {
        verificationReporter.reportResults(pact,
          interactionResults.values.fold(true) { acc, result -> acc && result }, lookupProviderVersion())
      }
    }
  }

  fun calculateInteractionHash(interaction: Interaction): Int {
    val builder = HashCodeBuilder().append(interaction.description)
    interaction.providerStates.forEach { builder.append(it.name) }
    return builder.toHashCode()
  }

  fun calculatePactHash(pact: Pact<Interaction>) =
    HashCodeBuilder().append(pact.consumer.name).append(pact.provider.name).toHashCode()

  fun lookupProviderVersion(): String {
    val version = System.getProperty("pact.provider.version")
    return if (version.isNullOrEmpty()) {
      logger.warn { "Set the provider version using the 'pact.provider.version' property. Defaulting to '0.0.0'" }
      "0.0.0"
    } else {
      version
    }
  }

  fun allInteractionsVerified(pact: Pact<Interaction>, results: MutableMap<Int, Boolean>): Boolean {
    return pact.interactions.all { results.containsKey(calculateInteractionHash(it)) }
  }
}
