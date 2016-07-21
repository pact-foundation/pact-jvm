package au.com.dius.pact.model

import groovy.transform.Canonical

/**
 * Class that encapsulates all the info about a provider state
 */
@Canonical
class ProviderState {
  /**
   * The provider state description
   */
  String name
  /**
   * Provider state parameters as key value pairs
   */
  Map<String, String> params = [:]

  static ProviderState fromMap(Map map) {
    new ProviderState(map)
  }

  Map toMap() {
    [
      name: name,
      params: params
    ]
  }
}
