package au.com.dius.pact.provider.gradle

/**
 * Config for pact broker
 */
data class Broker(
  var pactBrokerUrl: String? = null,
  var pactBrokerToken: String? = null,
  var pactBrokerUsername: String? = null,
  var pactBrokerPassword: String? = null,
  var pactBrokerAuthenticationScheme: String? = null,
  var retryCountWhileUnknown: Int? = null,
  var retryWhileUnknownInterval: Int? = null
) {
  override fun toString(): String {
    val password = if (pactBrokerPassword != null) "".padEnd(pactBrokerPassword!!.length, '*') else null
    return "Broker(pactBrokerUrl=$pactBrokerUrl, pactBrokerToken=$pactBrokerToken, " +
      "pactBrokerUsername=$pactBrokerUsername, pactBrokerPassword=$password, " +
      "pactBrokerAuthenticationScheme=$pactBrokerAuthenticationScheme, " +
      "retryCountWhileUnknown=$retryCountWhileUnknown, retryWhileUnknownInterval=$retryWhileUnknownInterval)"
  }
}
