package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.support.isNotEmpty
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueManager
import io.pact.plugins.jvm.core.DefaultPluginManager
import io.pact.plugins.jvm.core.MockServerDetails
import io.pact.plugins.jvm.core.MockServerResults
import mu.KLogging

/**
 * Mock server provided by a plugin
 */
class PluginMockServer(pact: BasePact, config: MockProviderConfig) : BaseMockServer(pact, config) {

  private var mockServerState: List<MockServerResults>? = null
  private lateinit var mockServerDetails: MockServerDetails
  private lateinit var mockServerEntry: CatalogueEntry

  override fun start() {
    mockServerEntry = CatalogueManager.lookupEntry(config.mockServerRegistryEntry)
      ?: throw InvalidMockServerRegistryEntry(config.mockServerRegistryEntry)
    mockServerDetails = DefaultPluginManager.startMockServer(mockServerEntry, config.toPluginMockServerConfig(), pact)
  }

  @Suppress("EmptyFunctionBlock")
  override fun waitForServer() { }

  override fun stop() {
    val response = DefaultPluginManager.shutdownMockServer(mockServerDetails)
    if (response.isNotEmpty()) {
      logger.warn { "Mock server returned an error response - $response" }
      this.mockServerState = response
    }
  }

  override fun getUrl() = mockServerDetails.baseUrl

  override fun getPort() = mockServerDetails.port

  override fun validateMockServerState(testResult: Any?): PactVerificationResult {
    return if (mockServerState.isNullOrEmpty()) {
      PactVerificationResult.Ok(testResult)
    } else {
      PactVerificationResult.Mismatches(mockServerState!!.map { results ->
        if (results.error.isNotEmpty()) {
          PactVerificationResult.Error(RuntimeException(results.error), PactVerificationResult.Ok(testResult))
        } else {
          PactVerificationResult.PartialMismatch(results.mismatches.map {
            BodyMismatch(it.expected, it.actual, it.mismatch, it.path, it.diff)
          })
        }
      })
    }
  }

  companion object : KLogging()
}

class InvalidMockServerRegistryEntry(private val registryEntry: String) :
  RuntimeException("Did not find an entry for '$registryEntry' in the plugin registry")
