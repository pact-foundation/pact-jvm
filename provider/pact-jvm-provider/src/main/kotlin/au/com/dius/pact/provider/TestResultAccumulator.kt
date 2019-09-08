package au.com.dius.pact.provider

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.pactbroker.TestResult
import au.com.dius.pact.provider.ProviderVerifier.Companion.PACT_VERIFIER_PUBLISH_RESULTS
import mu.KLogging
import org.apache.commons.lang3.builder.HashCodeBuilder

/**
 * Accumulates the test results for the interactions. Once all the interactions for a pact have been verified,
 * the result is submitted back to the broker
 */
interface TestResultAccumulator {
  @Deprecated(message = "Use the version that takes a TestResult parameter")
  fun updateTestResult(pact: Pact<out Interaction>, interaction: Interaction, testExecutionResult: Boolean)
  fun updateTestResult(pact: Pact<out Interaction>, interaction: Interaction, testExecutionResult: TestResult)

  fun clearTestResult(pact: Pact<out Interaction>)
}

object DefaultTestResultAccumulator : TestResultAccumulator, KLogging() {

  val testResults: MutableMap<Int, MutableMap<Int, TestResult>> = mutableMapOf()
  var verificationReporter: VerificationReporter = DefaultVerificationReporter

  override fun updateTestResult(pact: Pact<out Interaction>, interaction: Interaction, testExecutionResult: Boolean) {
    updateTestResult(pact, interaction, TestResult.fromBoolean(testExecutionResult))
  }

  override fun updateTestResult(
    pact: Pact<out Interaction>,
    interaction: Interaction,
    testExecutionResult: TestResult
  ) {
    logger.debug { "Received test result '$testExecutionResult' for Pact ${pact.provider.name}-${pact.consumer.name} " +
      "and ${interaction.description}" }
    val pactHash = calculatePactHash(pact)
    val interactionResults = testResults.getOrPut(pactHash) { mutableMapOf() }
    val interactionHash = calculateInteractionHash(interaction)
    val testResult = interactionResults[interactionHash]
    if (testResult == null) {
      interactionResults[interactionHash] = testExecutionResult
    } else {
      interactionResults[interactionHash] = testResult.merge(testExecutionResult)
    }
    if (allInteractionsVerified(pact, interactionResults)) {
      logger.debug {
        "All interactions for Pact ${pact.provider.name}-${pact.consumer.name} have a verification result"
      }
      if (verificationReporter.publishingResultsDisabled()) {
        logger.warn { "Skipping publishing of verification results as it has been disabled " +
          "($PACT_VERIFIER_PUBLISH_RESULTS is not 'true')" }
      } else {
        verificationReporter.reportResults(pact, interactionResults.values.fold(TestResult.Ok) {
          acc: TestResult, result -> acc.merge(result)
        }, lookupProviderVersion())
      }
      testResults.remove(pactHash)
    } else {
      logger.info { "Not all of the ${pact.interactions.size} were verified." }
    }
  }

  fun calculateInteractionHash(interaction: Interaction): Int {
    val builder = HashCodeBuilder().append(interaction.description)
    interaction.providerStates.forEach { builder.append(it.name) }
    return builder.toHashCode()
  }

  fun calculatePactHash(pact: Pact<out Interaction>) =
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

  fun allInteractionsVerified(pact: Pact<out Interaction>, results: MutableMap<Int, TestResult>): Boolean {
    logger.debug { "Number of interactions #${pact.interactions.size} and results: ${results.values}" }
    return pact.interactions.all { results.containsKey(calculateInteractionHash(it)) }
  }

  override fun clearTestResult(pact: Pact<out Interaction>) {
    val pactHash = calculatePactHash(pact)
    testResults.remove(pactHash)
  }
}
