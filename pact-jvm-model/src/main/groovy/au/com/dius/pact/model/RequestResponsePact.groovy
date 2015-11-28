package au.com.dius.pact.model

import groovy.transform.Canonical

/**
 * Pact between a consumer and a provider
 */
@Canonical
class RequestResponsePact extends BasePact {
  Provider provider
  Consumer consumer
  List<RequestResponseInteraction> interactions

  Pact sortInteractions() {
    interactions = new ArrayList(interactions).sort { it.providerState + it.description }
    this
  }

  RequestResponseInteraction interactionFor(String description, String providerState) {
    interactions.find { i ->
      i.description == description && i.providerState == providerState
    }
  }
}
