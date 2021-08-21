package au.com.dius.pact.provider

data class PactBrokerOptions @JvmOverloads constructor(
  /**
   * Enable pending pacts
   */
  val enablePending: Boolean = false,

  /**
   * Provider tags. Required if pending pacts are enabled
   */
  val providerTags: List<String> = listOf(),

  /**
   * Only include WIP pacts since the provided date
   */
  val includeWipPactsSince: String? = null
)
