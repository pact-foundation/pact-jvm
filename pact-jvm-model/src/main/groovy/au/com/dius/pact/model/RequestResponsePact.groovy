package au.com.dius.pact.model

import groovy.transform.CompileStatic

/**
 * Pact between a consumer and a provider
 */
@CompileStatic
class RequestResponsePact extends BasePact {
  List<RequestResponseInteraction> interactions

  RequestResponsePact(Provider provider, Consumer consumer, List<RequestResponseInteraction> interactions) {
    this(provider, consumer, interactions, DEFAULT_METADATA)
  }

  RequestResponsePact(Provider provider, Consumer consumer, List<RequestResponseInteraction> interactions,
                      Map metadata) {
    super(provider, consumer, metadata)
    this.interactions = interactions
  }

  Pact sortInteractions() {
    interactions = new ArrayList<RequestResponseInteraction>(interactions).sort { it.providerState + it.description }
    this
  }

  @Override
  @SuppressWarnings('SpaceAroundMapEntryColon')
  Map toMap(PactSpecVersion pactSpecVersion) {
    [
      provider      : objectToMap(provider),
      consumer      : objectToMap(consumer),
      interactions  : interactions*.toMap(pactSpecVersion),
      metadata      : metaData(pactSpecVersion >= PactSpecVersion.V3 ? '3.0.0' : '2.0.0')
    ]
  }

  RequestResponseInteraction interactionFor(String description, String providerState) {
    interactions.find { i ->
      i.description == description && i.providerStates.any { it.name == providerState }
    }
  }
}
