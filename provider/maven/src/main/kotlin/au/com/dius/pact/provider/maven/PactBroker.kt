package au.com.dius.pact.provider.maven

import java.net.URL

data class EnablePending @JvmOverloads constructor(val providerTags: List<String> = emptyList())

/**
 * Bean to configure a pact broker to query
 */
data class PactBroker @JvmOverloads constructor(
  val url: URL? = null,
  val tags: List<String>? = emptyList(),
  val authentication: PactBrokerAuth? = null,
  val serverId: String? = null,
  var enablePending: EnablePending? = null,
  val fallbackTag: String? = null
)

/**
 * Authentication for the pact broker, defaulting to Basic Authentication
 */
data class PactBrokerAuth @JvmOverloads constructor (
  val scheme: String? = "basic",
  val token: String? = null,
  val username: String? = null,
  val password: String? = null
)
