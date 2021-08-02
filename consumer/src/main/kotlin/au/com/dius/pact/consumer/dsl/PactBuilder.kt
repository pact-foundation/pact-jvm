package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.core.model.PactSpecVersion
import mu.KLogging

open class PactBuilder(
  var consumer: String = "consumer",
  var provider: String = "provider",
  var pactVersion: PactSpecVersion = PactSpecVersion.V4
) {
  /**
   * Use the old HTTP Pact DSL
   */
  fun usingLegacyDsl(): PactDslWithProvider {
    return PactDslWithProvider(ConsumerPactBuilder(consumer), provider, pactVersion)
  }

  /**
   * Sets the Pact specification version
   */
  fun pactSpecVersion(version: PactSpecVersion) {
    pactVersion = version
  }

  /**
   * Enable a plugin
   */
  @JvmOverloads
  fun usingPlugin(name: String, version: String? = null) {
    logger.debug("usingPlugin($name, $version)")
  }

  companion object : KLogging()
}
