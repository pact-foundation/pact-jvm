package au.com.dius.pact.provider.junit5

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.provider.reportVerificationResults
import mu.KLogging

object TestResultAccumulator : KLogging() {

  private val testResults: MutableMap<Pact<Interaction>, MutableMap<Interaction, Boolean>> = mutableMapOf()

  fun updateTestResult(
    pact: Pact<Interaction>,
    interaction: Interaction,
    testExecutionResult: Boolean
  ) {
    logger.debug { "Received test result '$testExecutionResult' for Pact ${pact.provider.name}-${pact.consumer.name} " +
      "and ${interaction.description}" }
    val interactionResults = testResults.getOrPut(pact) { mutableMapOf() }
    interactionResults[interaction] = testExecutionResult
    if (allInteractionsVerified(pact, interactionResults)) {
      logger.debug { "All interactions for Pact ${pact.provider.name}-${pact.consumer.name} are verified" }
      reportVerificationResults(pact, true, lookupProviderVersion(), null)
    }
  }

  fun lookupProviderVersion(): String {
    val version = System.getProperty("pact.provider.version")
    return if (version.isNullOrEmpty()) {
      logger.warn { "Set the provider version using the 'pact.provider.version' property. Defaulting to '0.0.0'" }
      "0.0.0"
    } else {
      version
    }
  }

  fun allInteractionsVerified(pact: Pact<Interaction>, results: MutableMap<Interaction, Boolean>): Boolean {
    return pact.interactions.all { results.getOrDefault(it, false) }
  }
}
