package au.com.dius.pact.provider

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.pactbroker.IPactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerClientConfig
import au.com.dius.pact.core.pactbroker.TestResult
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.core.support.mapError
import io.github.oshai.kotlinlogging.KLogging
import java.util.Locale

/**
 * Interface to the reporter that published the verification results
 */
interface VerificationReporter {
  /**
   * Publish the results to the pact broker. If the tag is given, then the provider will be tagged with that first.
   */
  @Deprecated("Use version that takes a list of provider tags")
  fun reportResults(
    pact: Pact,
    result: TestResult,
    version: String,
    client: IPactBrokerClient? = null,
    tag: String? = null
  )

  /**
   * Publish the results to the pact broker.
   * If the branch is given, then branch for this provider will be created first.
   * If the tags are given, then the provider will be tagged with those after the branch si created.
   */
  fun reportResults(
    pact: Pact,
    result: TestResult,
    version: String,
    client: IPactBrokerClient? = null,
    tags: List<String> = emptyList(),
    branch: String? = null
  ): Result<Boolean, List<String>>

  /**
   * This must return true unless the pact.verifier.publishResults property has the value of "true"
   */
  @Deprecated("Use version that takes a value resolver")
  fun publishingResultsDisabled(): Boolean

  /**
   * This must return true unless the pact.verifier.publishResults property has the value of "true"
   */
  fun publishingResultsDisabled(resolver: ValueResolver): Boolean
}

/**
 * Default implementation of a verification reporter
 */
object DefaultVerificationReporter : VerificationReporter, KLogging() {

  override fun reportResults(
    pact: Pact,
    result: TestResult,
    version: String,
    client: IPactBrokerClient?,
    tag: String?
  ) {
    if (tag.isNullOrEmpty()) {
      reportResults(pact, result, version, client, emptyList())
    } else {
      reportResults(pact, result, version, client, listOf(tag))
    }
  }

  override fun reportResults(
    pact: Pact,
    result: TestResult,
    version: String,
    client: IPactBrokerClient?,
    tags: List<String>,
    branch: String?
  ): Result<Boolean, List<String>> {
    return when (val source = pact.source) {
      is BrokerUrlSource -> {
        val brokerClient = client ?: PactBrokerClient(source.pactBrokerUrl, source.options.toMutableMap(),
          PactBrokerClientConfig())
        publishResult(brokerClient, source, result, version, pact, tags, branch)
      }
      else -> {
        logger.info { "Skipping publishing verification results for source $source" }
        Result.Ok(false)
      }
    }
  }

  private fun publishResult(
    brokerClient: IPactBrokerClient,
    source: BrokerUrlSource,
    result: TestResult,
    version: String,
    pact: Pact,
    tags: List<String>,
    branch: String?
  ): Result<Boolean, List<String>> {
    val branchResult = if (branch?.isNotBlank() == true) {
      brokerClient.publishProviderBranch(source.attributes, pact.provider.name, branch, version)
    } else {
      Result.Ok(true)
    }
    val tagsResult = if (tags.isNotEmpty()) {
      brokerClient.publishProviderTags(source.attributes, pact.provider.name, tags, version)
    } else {
      Result.Ok(true)
    }
    val buildUrl = System.getProperty(ProviderVerifier.PACT_VERIFIER_BUILD_URL)
    val publishResult = brokerClient.publishVerificationResults(source.attributes, result, version, buildUrl)
    if (publishResult is Result.Err) {
      logger.error { "Failed to publish verification results - ${publishResult.error}" }
    } else {
      logger.info { "Published verification result of '$result' for consumer '${pact.consumer}'" }
    }

    return when {
      tagsResult is Result.Err && branchResult is Result.Ok && publishResult is Result.Ok -> tagsResult
      branchResult is Result.Err && tagsResult is Result.Ok && publishResult is Result.Ok -> branchResult.mapError { listOf(it) }
      tagsResult is Result.Err && branchResult is Result.Err && publishResult is Result.Ok -> Result.Err(
        tagsResult.error + branchResult.error)
      tagsResult is Result.Err && branchResult is Result.Err && publishResult is Result.Err -> Result.Err(
        tagsResult.error + branchResult.error + publishResult.error)
      else -> publishResult.mapError { listOf(it) }
    }
  }

  override fun publishingResultsDisabled() = publishingResultsDisabled(SystemPropertyResolver)

  override fun publishingResultsDisabled(resolver: ValueResolver): Boolean {
    val property = resolver.resolveValue(ProviderVerifier.PACT_VERIFIER_PUBLISH_RESULTS, "false")
    return property?.lowercase(Locale.getDefault()) != "true"
  }
}
