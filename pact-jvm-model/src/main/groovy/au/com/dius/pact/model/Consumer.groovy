package au.com.dius.pact.model

import groovy.transform.Canonical

/**
 * Pact Consumer
 */
@Canonical
class Consumer {
  String name

  static Consumer fromMap(Map map) {
    new Consumer(map?.name ?: 'consumer')
  }
}
