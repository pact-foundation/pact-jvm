package au.com.dius.pact.provider.configuration

import groovy.json.JsonSlurper
import groovy.transform.Canonical

/**
 * Pact Configuration
 */
@Canonical
class PactConfiguration {
  Address providerRoot
  Address stateChangeUrl

  static PactConfiguration loadConfiguration(File configFile) {
    new JsonSlurper().parse(configFile) as PactConfiguration
  }
}
