package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import groovy.transform.Canonical

/**
 * Config for pact broker
 */
@Canonical
class PactBrokerConsumerConfig {
  List<ConsumerVersionSelector> selectors = []
  Boolean enablePending = false
  List<String> providerTags = []

  static List<ConsumerVersionSelector> latestTags(String... tags) {
    tags.collect {
      new ConsumerVersionSelector(it, true, null)
    }
  }
}
