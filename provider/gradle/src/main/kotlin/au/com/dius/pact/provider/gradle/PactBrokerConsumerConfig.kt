package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector

/**
 * Config for pact broker
 */
data class PactBrokerConsumerConfig @JvmOverloads constructor(
  var selectors: List<ConsumerVersionSelector>? = listOf(),
  var enablePending: Boolean? = false,
  var providerTags: List<String>? = listOf()
) {
  companion object {
    @JvmStatic
    @JvmOverloads
    fun latestTags(options: Map<String, Any> = mapOf(), vararg tags: String): List<ConsumerVersionSelector> {
      return tags.map {
        ConsumerVersionSelector(it, true, null, options["fallbackTag"]?.toString())
      }
    }
  }
}
