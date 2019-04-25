package au.com.dius.pact.provider.sbtsupport

import groovy.json.JsonSlurper
import java.io.File

/**
 * Address Configuration for SBT plugin
 */
data class Address @JvmOverloads constructor (
  val host: String?,
  val port: Int?,
  val path: String = "",
  val scheme: String = "http"
) {
  fun url() = "$scheme://$host:$port$path"
}

/**
 * Pact Configuration for SBT plugin
 */
data class PactConfiguration(val providerRoot: Address?, val stateChangeUrl: Address?) {

  fun validate() {
    if (providerRoot?.host == null || providerRoot.port == null) {
      throw InvalidPactConfigurationException("providerRoot is missing or invalid")
    }
  }

  companion object {
    @JvmStatic
    fun loadConfiguration(configFile: File): PactConfiguration {
      val configuration = JsonSlurper().parse(configFile) as PactConfiguration
      configuration.validate()
      return configuration
    }
  }
}

class InvalidPactConfigurationException(message: String) : RuntimeException(message)
