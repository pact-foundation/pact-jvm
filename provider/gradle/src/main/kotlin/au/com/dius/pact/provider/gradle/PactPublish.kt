package au.com.dius.pact.provider.gradle

/**
 * Config for pact publish task
 */
data class PactPublish @JvmOverloads constructor(
  var pactDirectory: Any? = null,
  var pactBrokerUrl: String? = null,
  @Deprecated("use consumerVersion")
  var providerVersion: Any? = null,
  var consumerVersion: Any? = null,
  var pactBrokerToken: String? = null,
  var pactBrokerUsername: String? = null,
  var pactBrokerPassword: String? = null,
  var pactBrokerAuthenticationScheme: String? = null,
  var tags: List<String> = listOf(),
  var excludes: List<String> = listOf()
)
