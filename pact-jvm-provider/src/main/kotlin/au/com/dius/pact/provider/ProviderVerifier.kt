package au.com.dius.pact.provider

import au.com.dius.pact.model.BrokerUrlSource
import au.com.dius.pact.model.Pact
import au.com.dius.pact.provider.broker.PactBrokerClient
import au.com.dius.pact.provider.broker.com.github.kittinunf.result.Result
import mu.KotlinLogging

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
      }
    }
  }
}
