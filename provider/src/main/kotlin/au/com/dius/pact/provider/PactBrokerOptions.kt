package au.com.dius.pact.provider

import au.com.dius.pact.core.support.Auth
import java.util.LinkedHashMap

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

  companion object {
    /**
     * Parse the authentication options provided from the build tools. These must be under an 'authentication' key
     * and must be either an instance of au.com.dius.pact.core.support.Auth or must be a list of strings, where the
     * first item is the scheme.
     */
    @JvmStatic
    @Suppress("TooGenericExceptionThrown", "ThrowsCount")
    fun parseAuthSettings(options: Map<String, Any?>): Auth? {
      return if (options.containsKey("authentication")) {
        when (val auth = options["authentication"]) {
          is Auth -> auth
          is List<*> -> if (auth.size > 1) {
            when (auth[0].toString().toLowerCase()) {
              "basic" -> if (auth.size > 2) {
                Auth.BasicAuthentication(auth[1]?.toString().orEmpty(), auth[2]?.toString().orEmpty())
              } else {
                Auth.BasicAuthentication(auth[1]?.toString().orEmpty(), "")
              }
              "bearer" -> {
                Auth.BearerAuthentication(auth[1]?.toString().orEmpty())
              }
              else -> throw RuntimeException("'${auth[0]}' ia not a valid authentication scheme. Only basic or " +
                "bearer is supported")
            }
          } else {
            throw RuntimeException("Authentication options must be a list of values with the first value being the " +
              "scheme, got '${options["authentication"]}'")
          }
          else -> {
            throw RuntimeException("Authentication options needs to be a Auth class or a list of values, " +
              "got '${options["authentication"]}'")
          }
        }
      } else null
    }
  }
}
