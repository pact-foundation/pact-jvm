package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.support.Auth

/**
 * Config for pact broker
 */
data class Broker(
  var pactBrokerUrl: String? = null,
  var pactBrokerToken: String? = null,
  var pactBrokerUsername: String? = null,
  var pactBrokerPassword: String? = null,
  var pactBrokerAuthenticationScheme: String? = null,
  var pactBrokerAuthenticationHeader: String = Auth.DEFAULT_AUTH_HEADER,
  var retryCountWhileUnknown: Int? = null,
  var retryWhileUnknownInterval: Int? = null,
  var pactBrokerInsecureTLS: Boolean? = null
) {
  override fun toString(): String {
    val password = if (pactBrokerPassword != null) "".padEnd(pactBrokerPassword!!.length, '*') else null
    return "Broker(pactBrokerUrl=$pactBrokerUrl, pactBrokerToken=$pactBrokerToken, " +
      "pactBrokerUsername=$pactBrokerUsername, pactBrokerPassword=$password, " +
      "pactBrokerAuthenticationScheme=$pactBrokerAuthenticationScheme, " +
      "pactBrokerAuthenticationHeader=$pactBrokerAuthenticationHeader, " +
      "pactBrokerInsecureTLS=$pactBrokerInsecureTLS, " +
      "retryCountWhileUnknown=$retryCountWhileUnknown, " +
      "retryWhileUnknownInterval=$retryWhileUnknownInterval)"
  }
}
