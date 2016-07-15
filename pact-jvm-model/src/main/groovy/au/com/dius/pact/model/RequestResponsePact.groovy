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
      interactions  : interactions.collect { interactionToMap(it, pactSpecVersion) },
      metadata      : metaData(pactSpecVersion >= PactSpecVersion.V3 ? '3.0.0' : '2.0.0')
    ]
  }

  @SuppressWarnings('SpaceAroundMapEntryColon')
  static Map interactionToMap(RequestResponseInteraction interaction, PactSpecVersion pactSpecVersion) {
    def interactionJson = [
      description  : interaction.description,
      request      : requestToMap(interaction.request, pactSpecVersion),
      response     : responseToMap(interaction.response)
    ]
    if (interaction.providerState) {
      interactionJson.providerState = interaction.providerState
    }
    interactionJson
  }

  static Map requestToMap(Request request, PactSpecVersion pactSpecVersion) {
    Map<String, Object> map = [
      method: request.method.toUpperCase() as Object,
      path: request.path as Object
    ]
    if (request.headers) {
      map.headers = request.headers as Map
    }
    if (request.query) {
      map.query = pactSpecVersion >= PactSpecVersion.V3 ? request.query : mapToQueryStr(request.query)
    }
    if (!request.body.missing) {
      map.body = parseBody(request)
    }
    if (request.matchingRules) {
      map.matchingRules = request.matchingRules
    }
    map
  }

  static Map responseToMap(Response response) {
    Map<String, Object> map = [status: response.status as Object]
    if (response.headers) {
      map.headers = response.headers as Map
    }
    if (!response.body.missing) {
      map.body = parseBody(response)
    }
    if (response.matchingRules) {
      map.matchingRules = response.matchingRules
    }
    map
  }

  RequestResponseInteraction interactionFor(String description, String providerState) {
    interactions.find { i ->
      i.description == description && i.providerState == providerState
    }
  }
}
