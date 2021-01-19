package au.com.dius.pact.provider.scalasupport

import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
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

  companion object {
    fun from(json: JsonValue): Address {
      val host = if (json.has("host")) {
        json["host"].toString()
      } else null
      val port = if (json.has("port")) {
        json["port"].asNumber()!!.toInt()
      } else null
      val path = if (json.has("path")) {
        json["path"].toString()
      } else ""
      val scheme = if (json.has("scheme")) {
        json["scheme"].toString()
      } else "http"
      return Address(host, port, path, scheme)
    }
  }
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
      val configurationJson = configFile.bufferedReader().use { JsonParser.parseReader(it) }
      val root = if (configurationJson.has("providerRoot")) {
        Address.from(configurationJson["providerRoot"])
      } else null
      val stateChangeUrl = if (configurationJson.has("stateChangeUrl")) {
        Address.from(configurationJson["stateChangeUrl"])
      } else null
      val configuration = PactConfiguration(root, stateChangeUrl)
      configuration.validate()
      return configuration
    }
  }
}

class InvalidPactConfigurationException(message: String) : RuntimeException(message)
