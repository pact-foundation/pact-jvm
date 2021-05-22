package au.com.dius.pact.provider

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.pactbroker.TestResult
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.provider.ProviderVerifier.Companion.PACT_VERIFIER_PUBLISH_RESULTS
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import mu.KLogging
import org.apache.commons.lang3.builder.HashCodeBuilder

/**
 * Accumulates the test results for the interactions. Once all the interactions for a pact have been verified,
 * the result is submitted back to the broker
 */
interface TestResultAccumulator {
  fun updateTestResult(
    pact: Pact<Interaction>,
    interaction: Interaction,
    testExecutionResult: List<VerificationResult>,
    source: PactSource,
    propertyResolver: ValueResolver = SystemPropertyResolver
  ): Result<Boolean, List<String>>
  fun updateTestResult(
    pact: Pact<out Interaction>,
    interaction: Interaction,
    testExecutionResult: TestResult,
    source: PactSource?,
    propertyResolver: ValueResolver = SystemPropertyResolver
  ): Result<Boolean, List<String>>
  fun clearTestResult(pact: Pact<out Interaction>, source: PactSource?)
}

object DefaultTestResultAccumulator : TestResultAccumulator, KLogging() {

  val testResults: MutableMap<Int, MutableMap<Int, TestResult>> = mutableMapOf()
  var verificationReporter: VerificationReporter = DefaultVerificationReporter

  override fun updateTestResult(
    pact: Pact<Interaction>,
    interaction: Interaction,
    testExecutionResult: List<VerificationResult>,
    source: PactSource,
    propertyResolver: ValueResolver
  ): Result<Boolean, List<String>> {
    val initial = TestResult.Ok(interaction.interactionId)
    return updateTestResult(pact, interaction, testExecutionResult.fold(initial) {
      acc: TestResult, r -> acc.merge(r.toTestResult())
    }, source, propertyResolver)
  }

  override fun updateTestResult(
    pact: Pact<out Interaction>,
    interaction: Interaction,
    testExecutionResult: TestResult,
    source: PactSource?,
    propertyResolver: ValueResolver
  ): Result<Boolean, List<String>> {
    logger.debug { "Received test result '$testExecutionResult' for Pact ${pact.provider.name}-${pact.consumer.name} " +
      "and ${interaction.description} (${source?.description()})" }
    val pactHash = calculatePactHash(pact, source)
    val interactionResults = testResults.getOrPut(pactHash) { mutableMapOf() }
    val interactionHash = calculateInteractionHash(interaction)
    val testResult = interactionResults[interactionHash]
    if (testResult == null) {
      interactionResults[interactionHash] = testExecutionResult
    } else {
      interactionResults[interactionHash] = testResult.merge(testExecutionResult)
    }
    val unverifiedInteractions = unverifiedInteractions(pact, interactionResults)
    return if (unverifiedInteractions.isEmpty()) {
      logger.debug {
        "All interactions for Pact ${pact.provider.name}-${pact.consumer.name} have a verification result"
      }
      val result = if (verificationReporter.publishingResultsDisabled(propertyResolver)) {
        logger.warn {
          "Skipping publishing of verification results as it has been disabled " +
            "($PACT_VERIFIER_PUBLISH_RESULTS is not 'true')"
        }
        Ok(false)
      } else {
        val initial = TestResult.Ok(interaction.interactionId)
        verificationReporter.reportResults(pact, interactionResults.values.fold(initial) { acc: TestResult, result ->
          acc.merge(result)
        }, lookupProviderVersion(), null, lookupProviderTags())
      }
      testResults.remove(pactHash)
      result
    } else {
      logger.warn { "Not all of the ${pact.interactions.size} were verified. The following were missing:" }
      unverifiedInteractions.forEach {
        logger.warn { "    ${it.description}" }
      }
      Ok(true)
    }
  }

  fun calculateInteractionHash(interaction: Interaction): Int {
    val builder = HashCodeBuilder().append(interaction.description)
    interaction.providerStates.forEach { builder.append(it.name) }
    return builder.toHashCode()
  }

  fun calculatePactHash(pact: Pact<out Interaction>, source: PactSource?): Int {
    val builder = HashCodeBuilder(91, 47).append(pact.consumer.name).append(pact.provider.name)

    if (source is BrokerUrlSource && source.url.isNotEmpty()) {
      builder.append(source.url)
    }

    return builder.toHashCode()
  }

  fun lookupProviderVersion(): String {
    val version = ProviderVersion { System.getProperty("pact.provider.version") }.get()
    return if (version.isNullOrEmpty()) {
      logger.warn { "Set the provider version using the 'pact.provider.version' property. Defaulting to '0.0.0'" }
      "0.0.0"
    } else {
      version
    }
  }

  private fun lookupProviderTags() = System.getProperty("pact.provider.tag").orEmpty().split(',')
    .map { it.trim() }.filter { it.isNotEmpty() }

  fun unverifiedInteractions(pact: Pact<out Interaction>, results: MutableMap<Int, TestResult>): List<Interaction> {
    logger.debug { "Number of interactions #${pact.interactions.size} and results: ${results.values}" }
    return pact.interactions.filter { !results.containsKey(calculateInteractionHash(it)) }
  }

  override fun clearTestResult(pact: Pact<out Interaction>, source: PactSource?) {
    val pactHash = calculatePactHash(pact, source)
    testResults.remove(pactHash)
  }
}
