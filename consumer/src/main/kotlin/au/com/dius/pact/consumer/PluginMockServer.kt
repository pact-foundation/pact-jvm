package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.support.isNotEmpty
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueManager
import io.pact.plugins.jvm.core.DefaultPluginManager
import io.pact.plugins.jvm.core.MockServerDetails
import io.pact.plugins.jvm.core.MockServerResults
import mu.KLogging

/**
 * Mock server provided by a plugin
 */
@Suppress("TooGenericExceptionThrown")
class PluginMockServer(pact: BasePact, config: MockProviderConfig) : BaseMockServer(pact, config) {

  private var mockServerState: List<MockServerResults>? = null
  private var mockServerDetails: MockServerDetails? = null
  private lateinit var transportEntry: CatalogueEntry

  override fun start() {
    transportEntry = CatalogueManager.lookupEntry(config.transportRegistryEntry)
      ?: throw InvalidMockServerRegistryEntry(config.transportRegistryEntry)
    mockServerDetails = DefaultPluginManager.startMockServer(transportEntry, config.toPluginMockServerConfig(), pact)
  }

  @Suppress("EmptyFunctionBlock")
  override fun waitForServer() { }

  override fun stop() {
    if (mockServerDetails != null) {
      val response = DefaultPluginManager.shutdownMockServer(mockServerDetails!!)
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
    return when (val p = pact.asV4Pact()) {
      is Ok -> {
        for (interaction in p.value.interactions) {
          interaction.asV4Interaction().transport = transportEntry.key
        }
        p.value
      }
      is Err -> pact
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
