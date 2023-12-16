package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.V4Interaction
import spock.lang.Specification
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.messaging.Message

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
}
