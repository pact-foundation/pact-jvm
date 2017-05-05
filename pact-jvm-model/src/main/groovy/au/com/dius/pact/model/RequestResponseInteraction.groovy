package au.com.dius.pact.model

import groovy.transform.Canonical

/**
 * Interaction between a consumer and a provider
 */
@Canonical
class RequestResponseInteraction implements Interaction {

  String description
  String providerState
  Request request
  Response response

  @Override
  String toString() {
    "Interaction: $description\n\tin state ${displayState()}\nrequest:\n$request\n\nresponse:\n$response"
  }

  String displayState() {
    if (providerState == null || providerState.empty) {
      'None'
    } else {
      providerState
    }
  }

  @Override
  boolean conflictsWith(Interaction other) {
//    description == other.description &&
//      providerState == other.providerState &&
//      (request != other.request || response != other.response)
    false
  }

  @Override
  String uniqueKey() {
    "${displayState()}_$description"
  }
}
