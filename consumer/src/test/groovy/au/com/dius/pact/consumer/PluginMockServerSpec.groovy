package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.consumer.model.MockServerImplementation
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import io.pact.plugin.Plugin
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueEntryProviderType
import io.pact.plugins.jvm.core.CatalogueEntryType
import io.pact.plugins.jvm.core.CatalogueManager
import io.pact.plugins.jvm.core.MockServerConfig
import io.pact.plugins.jvm.core.MockServerDetails
import io.pact.plugins.jvm.core.MockServerResults
import io.pact.plugins.jvm.core.PactPlugin
import io.pact.plugins.jvm.core.PluginManager
import spock.lang.Specification

class PluginMockServerSpec extends Specification {
  BasePact pact
  MockProviderConfig config
  PluginMockServer mockServer
  PluginManager pluginManager
  Plugin.CatalogueEntry catalogueEntry
  PactPlugin plugin

  def setup() {
    pact = Mock(BasePact)
    config = new MockProviderConfig('127.0.0.1', 0, PactSpecVersion.V3, 'http', MockServerImplementation.JavaHttpServer,
      false, 'plugin/test/transport/test')
    pluginManager = Mock(PluginManager)
    catalogueEntry = Plugin.CatalogueEntry.newBuilder()
      .setType(Plugin.CatalogueEntry.EntryType.TRANSPORT)
      .setKey('test')
      .build()
    CatalogueManager.INSTANCE.registerPluginEntries('test', [ catalogueEntry ])
    plugin = Mock()
  }

  def 'start - looks up the transport in the catalogue'() {
    given:
    def mockServerConfig = new MockServerConfig('127.0.0.1', 0, false)
    def expectedEntry = new CatalogueEntry(CatalogueEntryType.TRANSPORT, CatalogueEntryProviderType.PLUGIN,
      'test', 'test')
    mockServer = new PluginMockServer(pact, config)
    mockServer.pluginManager = pluginManager

    when:
    mockServer.start()

    then:
    1 * pluginManager.startMockServer(expectedEntry, mockServerConfig, pact)
    mockServer.transportEntry == expectedEntry
  }

  def 'start - if the transport does not contain a slash, prefix transport to the lookup'() {
    given:
    config = new MockProviderConfig('127.0.0.1', 0, PactSpecVersion.V3, 'http', MockServerImplementation.JavaHttpServer,
      false, 'test')
    def mockServerConfig = new MockServerConfig('127.0.0.1', 0, false)
    def expectedEntry = new CatalogueEntry(CatalogueEntryType.TRANSPORT, CatalogueEntryProviderType.PLUGIN,
      'test', 'test')
    mockServer = new PluginMockServer(pact, config)
    mockServer.pluginManager = pluginManager

    when:
    mockServer.start()

    then:
    1 * pluginManager.startMockServer(expectedEntry, mockServerConfig, pact)
    mockServer.transportEntry == expectedEntry
  }

  def 'start - throw an exception if the entry is not found'() {
    given:
    config = new MockProviderConfig('127.0.0.1', 0, PactSpecVersion.V3, 'http', MockServerImplementation.JavaHttpServer,
      false, 'some-other-test')
    mockServer = new PluginMockServer(pact, config)
    mockServer.pluginManager = pluginManager

    when:
    mockServer.start()

    then:
    thrown(InvalidMockServerRegistryEntry)
  }

  def 'stop - shuts the mock server down and stores the results'() {
    given:
    mockServer = new PluginMockServer(pact, config)
    mockServer.pluginManager = pluginManager

    def mockServerDetails = new MockServerDetails('test', 'http://127.0.0.1', 1234, plugin)
    mockServer.mockServerDetails = mockServerDetails

    def result = new MockServerResults('test.path', null, [])

    when:
    mockServer.stop()

    then:
    1 * pluginManager.shutdownMockServer(mockServerDetails) >> [ result ]
    mockServer.mockServerState == [ result ]
  }

  def 'returns the host address and port received from the running mock server'() {
    given:
    mockServer = new PluginMockServer(pact, config)
    def mockServerDetails = new MockServerDetails('test', 'xpx://100.0.0.1', 1234, plugin)

    when:
    mockServer.mockServerDetails = mockServerDetails

    then:
    mockServer.url == 'xpx://100.0.0.1'
    mockServer.port == 1234
  }

  def 'update pact sets the transport for V4 pacts'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('test int', 'test int')
    pact = new V4Pact(new Consumer('test mock'), new Provider('test mock'), [ interaction ])
    mockServer = new PluginMockServer(pact, config)
    mockServer.pluginManager = pluginManager

    when:
    mockServer.start()
    mockServer.updatePact(pact)

    then:
    interaction.transport == 'test'
  }

  def 'update pact does nothing for V3 and lower pacts'() {
    given:
    def interaction = new RequestResponseInteraction('test int')
    pact = new RequestResponsePact(new Provider('test mock'), new Consumer('test mock'), [interaction ])
    mockServer = new PluginMockServer(pact, config)
    mockServer.pluginManager = pluginManager

    when:
    mockServer.start()
    def result = mockServer.updatePact(pact)

    then:
    result == pact
  }

  def 'validateMockServerState - returns an OK result if the state from the mock server is empty'() {
    given:
    mockServer = new PluginMockServer(pact, config)
    mockServer.mockServerState = []

    when:
    def result = mockServer.validateMockServerState(true)

    then:
    result == new PactVerificationResult.Ok(true)
  }

  def 'validateMockServerState - returns a mismatch result if the state from the mock server is not empty'() {
    given:
    mockServer = new PluginMockServer(pact, config)
    mockServer.mockServerState = [
      new MockServerResults('test.path', 'boom!', [])
    ]

    when:
    def result = mockServer.validateMockServerState(true)

    then:
    result instanceof PactVerificationResult.Mismatches
    result.mismatches[0] instanceof PactVerificationResult.Error
    result.mismatches[0].error.message == 'boom!'
  }
}
