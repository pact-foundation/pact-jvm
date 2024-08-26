package au.com.dius.pact.provider.junit5

import spock.lang.Specification
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.matchers.generators.ArrayContainsJsonGenerator
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.support.Result
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueEntryProviderType
import io.pact.plugins.jvm.core.CatalogueEntryType
import io.pact.plugins.jvm.core.InteractionVerificationData
import io.pact.plugins.jvm.core.PluginManager

class PluginTestTargetSpec extends Specification {
  def 'supports any V4 interaction'() {
    expect:
    new PluginTestTarget().supportsInteraction(interaction) == result

    where:
    interaction                                   | result
    new RequestResponseInteraction('test')        | false
    new Message('test')                           | false
    new V4Interaction.AsynchronousMessage('test') | true
    new V4Interaction.SynchronousMessages('test') | true
    new V4Interaction.SynchronousHttp('test')     | true
  }

  def 'only supports interactions that have a matching transport'() {
    given:
    def interaction1 = new V4Interaction.SynchronousHttp('test')
    interaction1.transport = 'http'
    def interaction2 = new V4Interaction.SynchronousHttp('test')
    interaction2.transport = 'xttp'
    def pluginTarget = new PluginTestTarget([transport: 'xttp'])

    expect:
    !pluginTarget.supportsInteraction(interaction1)
    pluginTarget.supportsInteraction(interaction2)
  }

  def 'when calling a plugin, prepareRequest must merge the provider state test context config'() {
    given:
    def config = [
      transport: 'grpc',
      host: 'localhost',
      port: 38525
    ]
    def target = new PluginTestTarget(config)
    target.transportEntry = new CatalogueEntry(CatalogueEntryType.CONTENT_MATCHER, CatalogueEntryProviderType.PLUGIN,
      'null', 'null')
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction')
    def pact = new V4Pact(new Consumer(), new Provider(), [ interaction ])
    def context = [
      providerState: [a: 100, b: 200],
      ArrayContainsJsonGenerator: ArrayContainsJsonGenerator.INSTANCE
    ]
    def expectedContext = [
      transport: 'grpc',
      host: 'localhost',
      port: 38525,
      providerState: [a: 100, b: 200]
    ]
    def pluginManager = Mock(PluginManager)
    target.pluginManager = pluginManager

    when:
    target.prepareRequest(pact, interaction, context)

    then:
    noExceptionThrown()
    1 * pluginManager.prepareValidationForInteraction(_, _, _, expectedContext) >> new Result.Ok(
      new InteractionVerificationData(OptionalBody.missing(), [:]))
  }
}
