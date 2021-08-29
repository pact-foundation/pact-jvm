package au.com.dius.pact.provider

import au.com.dius.pact.core.support.Auth

data class PactBrokerOptions @JvmOverloads constructor(
  /**
   * Enable pending pacts.
   * See https://docs.pact.io/pact_broker/advanced_topics/pending_pacts
   */
  val enablePending: Boolean = false,

  /**
   * Provider tags. Required if pending pacts are enabled
   */
  val providerTags: List<String> = listOf(),

  /**
   * Only include WIP pacts since the provided date. Dates need to be in ISO format (YYYY-MM-DD).
   * See https://docs.pact.io/pact_broker/advanced_topics/wip_pacts/
   */
  val includeWipPactsSince: String? = null,

  /**
   * If we should enable insecure TLS. This will disable certificate hostname checks and accept all certificates.
   */
  val insecureTLS: Boolean = false,

  /**
   * Authentication for the pact broker
   */
  val auth: Auth? = null
) {
  @Deprecated("This will be removed once autentication options are moved to PactBrokerClientConfig")
  fun toMutableMap(): MutableMap<String, Any> {
    return if (auth != null) {
      mutableMapOf("authentication" to auth)
    } else {
      mutableMapOf()
    }
  }
}
