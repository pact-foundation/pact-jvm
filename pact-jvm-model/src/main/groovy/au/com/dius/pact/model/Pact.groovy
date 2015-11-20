package au.com.dius.pact.model

import groovy.transform.Canonical

/**
 * Pact between a consumer and a provider
 */
@Canonical
class Pact extends BasePact {
  Provider provider
  Consumer consumer
  List<Interaction> interactions

  void sortInteractions() {
    interactions = new ArrayList(interactions).sort { it.providerState + it.description }
  }

  Interaction interactionFor(String description, String providerState) {
    interactions.find { i ->
      i.description == description && i.providerState == providerState
    }
  }
}
