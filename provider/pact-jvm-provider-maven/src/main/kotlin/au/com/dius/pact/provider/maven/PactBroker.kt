package au.com.dius.pact.provider.maven

import java.net.URL

/**
 * Bean to configure a pact broker to query
 */
data class PactBroker(
  val url: URL?,
  val tags: List<String>? = emptyList(),
  val authentication: PactBrokerAuth?,
  val serverId: String?
)

/**
 * Authentication for the pact broker, defaulting to Basic Authentication
 */
data class PactBrokerAuth(
  val scheme: String? = "basic",
  val token: String?,
  val username: String?,
  val password: String?
)
