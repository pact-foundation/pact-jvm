package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerClientConfig
import au.com.dius.pact.core.support.isNotEmpty
import org.gradle.api.DefaultTask

open class PactCanIDeployBaseTask : DefaultTask() {
  companion object {
    @JvmStatic
    fun setupBrokerClient(brokerConfig: Broker): PactBrokerClient {
      val options = mutableMapOf<String, Any>()
      if (brokerConfig.pactBrokerToken.isNotEmpty()) {
        options["authentication"] = listOf(brokerConfig.pactBrokerAuthenticationScheme ?: "bearer",
          brokerConfig.pactBrokerToken)
      } else if (brokerConfig.pactBrokerUsername.isNotEmpty()) {
        options["authentication"] = listOf(brokerConfig.pactBrokerAuthenticationScheme ?: "basic",
          brokerConfig.pactBrokerUsername, brokerConfig.pactBrokerPassword)
      }

      val config = when {
        brokerConfig.retryCountWhileUnknown != null -> PactBrokerClientConfig(brokerConfig.retryCountWhileUnknown!!,
          brokerConfig.retryWhileUnknownInterval ?: 10)
        else -> PactBrokerClientConfig()
      }

      return PactBrokerClient(brokerConfig.pactBrokerUrl!!, options, config)
    }
  }
}
