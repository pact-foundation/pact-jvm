package au.com.dius.pact.provider

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.pactbroker.IPactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.TestResult
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
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
    pact: Pact,
    result: TestResult,
    version: String,
    client: IPactBrokerClient? = null,
    tag: String? = null
  )

  /**
   * Publish the results to the pact broker. If the tags are given, then the provider will be tagged with those first.
   */
  fun reportResults(
    pact: Pact,
    result: TestResult,
    version: String,
    client: IPactBrokerClient? = null,
    tags: List<String> = emptyList()
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
    tags: List<String>
  ): Result<Boolean, List<String>> {
    return when (val source = pact.source) {
      is BrokerUrlSource -> {
        val brokerClient = client ?: PactBrokerClient(source.pactBrokerUrl, source.options.toMutableMap())
        publishResult(brokerClient, source, result, version, pact, tags)
      }
      else -> {
        logger.info { "Skipping publishing verification results for source $source" }
        Ok(false)
      }
    }
  }

  private fun publishResult(
    brokerClient: IPactBrokerClient,
    source: BrokerUrlSource,
    result: TestResult,
    version: String,
    pact: Pact,
    tags: List<String>
  ): Result<Boolean, List<String>> {
    val tagsResult = if (tags.isNotEmpty()) {
      brokerClient.publishProviderTags(source.attributes, pact.provider.name, tags, version)
    } else {
      Ok(true)
    }
    val publishResult = brokerClient.publishVerificationResults(source.attributes, result, version)
    if (publishResult is Err) {
      logger.error { "Failed to publish verification results - ${publishResult.error}" }
    } else {
      logger.info { "Published verification result of '$result' for consumer '${pact.consumer}'" }
    }

    return when {
      tagsResult is Err && publishResult is Ok -> tagsResult
      tagsResult is Err && publishResult is Err -> Err(tagsResult.error + publishResult.error)
      else -> publishResult.mapError { listOf(it) }
    }
  }

  override fun publishingResultsDisabled() = publishingResultsDisabled(SystemPropertyResolver)

  override fun publishingResultsDisabled(resolver: ValueResolver): Boolean {
    val property = resolver.resolveValue(ProviderVerifier.PACT_VERIFIER_PUBLISH_RESULTS, "false")
    return property?.toLowerCase() != "true"
  }
}
