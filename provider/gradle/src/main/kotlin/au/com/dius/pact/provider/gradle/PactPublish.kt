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
  var excludes: List<String> = listOf(),
  var consumerBranch: String? = null,
  var consumerBuildUrl: String? = null
) {
  override fun toString(): String {
    val password = if (pactBrokerPassword != null) "".padEnd(pactBrokerPassword!!.length, '*') else null
    return "PactPublish(pactDirectory=$pactDirectory, pactBrokerUrl=$pactBrokerUrl, " +
      "providerVersion=$providerVersion, consumerVersion=$consumerVersion, pactBrokerToken=$pactBrokerToken, " +
      "pactBrokerUsername=$pactBrokerUsername, pactBrokerPassword=$password, " +
      "pactBrokerAuthenticationScheme=$pactBrokerAuthenticationScheme, tags=$tags, excludes=$excludes, " +
      "consumerBranch=$consumerBranch, consumerBuildUrl=$consumerBuildUrl)"
  }
}
