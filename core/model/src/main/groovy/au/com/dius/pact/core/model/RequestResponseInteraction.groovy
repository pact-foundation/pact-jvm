package au.com.dius.pact.core.model

import groovy.json.JsonSlurper
import groovy.transform.Canonical

/**
 * Interaction between a consumer and a provider
 */
@Canonical
class RequestResponseInteraction implements Interaction {

  private static final String COMMA = ', '

  String description
  List<ProviderState> providerStates = []
  Request request
  Response response

  @Override
  String toString() {
    "Interaction: $description\n\tin states ${displayState()}\nrequest:\n$request\n\nresponse:\n$response"
  }

  String displayState() {
    if (providerStates.empty || providerStates.size() == 1 && !providerStates[0].name) {
      'None'
    } else {
      providerStates*.name.join(COMMA)
    }
  }

  @Override
  @Deprecated
  String getProviderState() {
    providerStates.empty ? null : providerStates.first().name
  }

  @Override
  boolean conflictsWith(Interaction other) {
//    description == other.description &&
//      providerStates == other.providerStates &&
//      (request != other.request || response != other.response)
    false
  }

  @Override
  String uniqueKey() {
    "${displayState()}_$description"
  }

  @Override
  @SuppressWarnings('SpaceAroundMapEntryColon')
  Map toMap(PactSpecVersion pactSpecVersion = PactSpecVersion.V3) {
    def interactionJson = [
      description     : description,
      request         : requestToMap(request, pactSpecVersion),
      response        : responseToMap(response, pactSpecVersion)
    ]
    if (pactSpecVersion < PactSpecVersion.V3 && providerStates) {
      interactionJson.providerState = providerState
    } else if (providerStates) {
      interactionJson.providerStates = providerStates*.toMap()
    }
    interactionJson
  }

  static Map requestToMap(Request request, PactSpecVersion pactSpecVersion) {
    Map<String, Object> map = [
      method: request.method.toUpperCase() as Object,
      path: request.path as Object
    ]
    if (request.headers) {
      map.headers = (request.headers as Map).collectEntries { key, value -> [key, value.join(COMMA)] }
    }
    if (request.query) {
      map.query = pactSpecVersion >= PactSpecVersion.V3 ? request.query : mapToQueryStr(request.query)
    }
    if (!request.body.missing) {
      map.body = parseBody(request)
    }
    if (request.matchingRules?.notEmpty) {
      map.matchingRules = request.matchingRules.toMap(pactSpecVersion)
    }
    if (request.generators?.notEmpty && pactSpecVersion >= PactSpecVersion.V3) {
      map.generators = request.generators.toMap(pactSpecVersion)
    }

    map
  }

  static Map responseToMap(Response response, PactSpecVersion pactSpecVersion) {
    Map<String, Object> map = [status: response.status as Object]
    if (response.headers) {
      map.headers = (response.headers as Map).collectEntries { key, value -> [key, value.join(COMMA)] }
    }
    if (!response.body.missing) {
      map.body = parseBody(response)
    }
    if (response.matchingRules?.notEmpty) {
      map.matchingRules = response.matchingRules.toMap(pactSpecVersion)
    }
    if (response.generators?.notEmpty && pactSpecVersion >= PactSpecVersion.V3) {
      map.generators = response.generators.toMap(pactSpecVersion)
    }
    map
  }

  static String mapToQueryStr(Map<String, List<String>> query) {
    query.collectMany { k, v -> v.collect { "$k=${URLEncoder.encode(it, 'UTF-8')}" } }.join('&')
  }

  static parseBody(HttpPart httpPart) {
    if (httpPart.jsonBody() && httpPart.body.present) {
      def body = new JsonSlurper().parseText(httpPart.body.valueAsString())
      if (body instanceof String) {
        httpPart.body.valueAsString()
      } else {
        body
      }
    } else {
      httpPart.body.valueAsString()
    }
  }

}
