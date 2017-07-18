package au.com.dius.pact.provider.sbtsupport

import groovy.json.JsonSlurper
import groovy.transform.Canonical

/**
 * Pact Configuration for SBT plugin
 */
@Canonical
class PactConfiguration {
  Address providerRoot
  Address stateChangeUrl

  static PactConfiguration loadConfiguration(File configFile) {
    def configuration = new JsonSlurper().parse(configFile) as PactConfiguration
    configuration.validate()
    configuration
  }

  void validate() {
    if (providerRoot == null || providerRoot.host == null || providerRoot.port == null) {
      throw new InvalidPactConfigurationException('providerRoot is missing or invalid')
    }
  }
}
