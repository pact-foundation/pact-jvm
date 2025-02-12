package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.MetadataMismatch
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.support.contains
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.json.JsonValue
import au.com.dius.pact.core.support.toJson
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueManager
import io.pact.plugins.jvm.core.DefaultPluginManager
import io.pact.plugins.jvm.core.MockServerDetails
import io.pact.plugins.jvm.core.MockServerResults
import io.pact.plugins.jvm.core.PluginManager
import io.github.oshai.kotlinlogging.KLogging

/**
 * Mock server provided by a plugin. Any additional transport configuration will be passed to the plugin
 * mock server in the test context under the "transport_config" key.
 */
@Suppress("TooGenericExceptionThrown")
class PluginMockServer(pact: BasePact, config: MockProviderConfig) : BaseMockServer(pact, config) {

  private var mockServerState: List<MockServerResults>? = null
  private var mockServerDetails: MockServerDetails? = null
  private lateinit var transportEntry: CatalogueEntry

  /**
   * Public for testing
   */
  var pluginManager: PluginManager = DefaultPluginManager

  override fun start() {
    val entry = if (config.transportRegistryEntry.contains("/")) {
      CatalogueManager.lookupEntry(config.transportRegistryEntry)
    } else {
      CatalogueManager.lookupEntry("transport/" + config.transportRegistryEntry)
    }
    if (entry == null) {
      throw InvalidMockServerRegistryEntry(config.transportRegistryEntry)
    }
    val testContext = mutableMapOf<String, JsonValue>()
    if (config.transportConfig.isNotEmpty()) {
      testContext["transport_config"] = config.transportConfig.toJson()
    }

    transportEntry = entry
    mockServerDetails = pluginManager.startMockServer(transportEntry, config.toPluginMockServerConfig(), pact, testContext)
  }

  @Suppress("EmptyFunctionBlock")
  override fun waitForServer() { }

  override fun stop() {
    if (mockServerDetails != null) {
      val response = pluginManager.shutdownMockServer(mockServerDetails!!)
      if (response.isNotEmpty()) {
        logger.warn { "Mock server returned an error response - $response" }
        this.mockServerState = response
      }
    } else {
      throw RuntimeException("Mock server is not running")
    }
  }

  override fun getUrl() = mockServerDetails?.baseUrl ?: throw RuntimeException("Mock server is not running")

  override fun getPort() = mockServerDetails?.port ?: throw RuntimeException("Mock server is not running")

  override fun updatePact(pact: Pact): Pact {
    return if (pact.isV4Pact()) {
      when (val p = pact.asV4Pact()) {
        is Result.Ok -> {
          for (interaction in p.value.interactions) {
            interaction.asV4Interaction().transport = transportEntry.key
          }
          p.value
        }
        is Result.Err -> pact
      }
    } else {
      pact
    }
  }

  override fun validateMockServerState(testResult: Any?): PactVerificationResult {
    return if (mockServerState.isNullOrEmpty()) {
      PactVerificationResult.Ok(testResult)
    } else {
      PactVerificationResult.Mismatches(mockServerState!!.map { results ->
        if (results.error.isNotEmpty()) {
          PactVerificationResult.Error(RuntimeException(results.error), PactVerificationResult.Ok(testResult))
        } else {
          PactVerificationResult.PartialMismatch(results.mismatches.map {
            if (it.mismatchType == "metadata") {
              MetadataMismatch(it.path, it.expected, it.actual, it.mismatch)
            } else {
              BodyMismatch(it.expected, it.actual, it.mismatch, it.path, it.diff)
            }
          })
        }
      })
    }
  }

  companion object : KLogging()
}

class InvalidMockServerRegistryEntry(private val registryEntry: String) :
  RuntimeException("Did not find an entry for '$registryEntry' in the plugin registry")
