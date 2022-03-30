package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderResponse
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueManager
import io.pact.plugins.jvm.core.DefaultPluginManager
import io.pact.plugins.jvm.core.InteractionVerificationData
import io.pact.plugins.jvm.core.PactPluginNotFoundException
import java.io.File
import java.net.URL

/**
 * Provider data when verifying via a plugin
 */
data class PluginProvider(
  /**
   * Provider name
   */
  override var name: String,

  /**
   * Source that the Pact will be loaded from
   */
  val pactSource: PactSource?,

  /**
   * User provided configuration
   */
  val config: MutableMap<String, Any?>
) : IProviderInfo {
  override var protocol: String
    get() = config.getOrDefault("transport", "http").toString()
    set(value) {
      config["transport"] = value
    }

  override var host: Any?
    get() = config.getOrDefault("host", "localhost")
    set(value) {
      config["host"] = value
    }

  override var port: Any?
    get() = config.getOrDefault("port", 8080)
    set(value) {
      config["port"] = value
    }

  override var verificationType: PactVerification? = PactVerification.PLUGIN

  override var path: String = ""
  override val requestFilter: Any? = null
  override val stateChangeRequestFilter: Any? = null
  override val stateChangeUrl: URL? = null
  override val stateChangeUsesBody: Boolean = true
  override val stateChangeTeardown: Boolean = false
  override var packagesToScan: List<String> = emptyList()
  override var createClient: Any? = null
  override var insecure: Boolean = false
  override var trustStore: File? = null
  override var trustStorePassword: String? = null
}

/**
 * Test target were the verification will be provided by a plugin
 */
class PluginTestTarget(private val config: MutableMap<String, Any?> = mutableMapOf()) : TestTarget {
  private lateinit var transportEntry: CatalogueEntry

  override val userConfig: Map<String, Any?>
    get() = config

  override fun getProviderInfo(serviceName: String, pactSource: PactSource?): IProviderInfo {
    return PluginProvider(serviceName, pactSource, config)
  }

  override fun prepareRequest(pact: Pact, interaction: Interaction, context: MutableMap<String, Any>): Pair<Any, Any?>? {
    return when (val v4pact = pact.asV4Pact()) {
      is Ok -> when (val result = DefaultPluginManager.prepareValidationForInteraction(transportEntry, v4pact.value,
        interaction.asV4Interaction(), config)) {
        is Ok -> result.value to transportEntry
        is Err -> throw RuntimeException("Failed to configure the interaction for verification - ${result.error}")
      }
      is Err -> throw RuntimeException("PluginTestTarget can only be used with V4 Pacts")
    }
  }

  override fun isHttpTarget(): Boolean {
    return false
  }

  override fun executeInteraction(client: Any?, request: Any?): ProviderResponse {
    return ProviderResponse()
  }

  override fun prepareVerifier(verifier: IProviderVerifier, testInstance: Any, pact: Pact) {
    if (pact.isV4Pact()) {
      when (val v4pact = pact.asV4Pact()) {
        is Ok -> {
          for (plugin in v4pact.value.pluginData()) {
            when (DefaultPluginManager.loadPlugin(plugin.name, plugin.version)) {
              is Ok -> {}
              is Err -> throw PactPluginNotFoundException(plugin.name, plugin.version)
            }
          }
          val transport = config["transport"]
          if (transport is String) {
            val entry = CatalogueManager.lookupEntry("transport/$transport")
            if (entry != null) {
              transportEntry = entry
            } else {
              throw RuntimeException("Did not find a registered transport '$transport'")
            }
          } else {
            throw RuntimeException("PluginTestTarget requires the transport to be configured")
          }
        }
        is Err -> throw RuntimeException("PluginTestTarget can only be used with V4 Pacts")
      }
    } else {
      throw RuntimeException("PluginTestTarget can only be used with V4 Pacts")
    }
  }
}
