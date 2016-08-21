package au.com.dius.pact.model.v3.messaging

import au.com.dius.pact.model.HttpPart
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.Response
import au.com.dius.pact.model.matchingrules.MatchingRules
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Canonical

/**
 * Message in a Message Pact
 */
@Canonical
class Message implements Interaction {
  private static final String JSON = 'application/json'

  String description
  List<ProviderState> providerStates = []
  OptionalBody contents = OptionalBody.missing()
  MatchingRules matchingRules = new MatchingRules()
  Map<String, String> metaData = [:]

  byte[] contentsAsBytes() {
    if (contents.present) {
      contents.value.toString().bytes
    } else {
      []
    }
  }

  String getContentType() {
    metaData.contentType ?: JSON
  }

  @SuppressWarnings('UnusedMethodParameter')
  Map toMap(PactSpecVersion pactSpecVersion = PactSpecVersion.V3) {
    def map = [
      description: description
    ]
    if (!contents.missing) {
      if (metaData.contentType == JSON) {
        map.contents = new JsonSlurper().parseText(contents.value.toString())
      } else {
        map.contents = contentsAsBytes().encodeBase64().toString()
      }
    }
    if (providerState) {
      map.providerState = providerState
    }
    if (matchingRules) {
      map.matchingRules = matchingRules
    }
    map
  }

  /**
   * Builds a message from a Map
   */
  static Message fromMap(Map map) {
    Message message = new Message()
    message.description = map.description ?: ''
    if (map.providerStates) {
      message.providerStates = map.providerStates.collect { ProviderState.fromMap(it) }
    } else {
      message.providerStates = map.providerState ? [ new ProviderState(map.providerState.toString()) ] : []
    }
    if (map.containsKey('contents')) {
      if (map.contents == null) {
        message.contents = OptionalBody.nullBody()
      } else if (map.contents instanceof String && map.contents.empty) {
        message.contents = OptionalBody.empty()
      } else {
        message.contents = OptionalBody.body(JsonOutput.toJson(map.contents))
      }
    }
    message.matchingRules = MatchingRules.fromMap(map.matchingRules)
    message.metaData = map.metaData ?: [:]
    message
  }

  HttpPart asPactRequest() {
    new Response(200, ['Content-Type': contentType], contents, matchingRules)
  }

  @Override
  @Deprecated
  String getProviderState() {
    providerStates.isEmpty() ? null : providerStates.first().name
  }

  @Override
  boolean conflictsWith(Interaction other) {
    false
  }
}
