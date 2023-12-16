package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.messaging.Message
import spock.lang.Specification

class MessageTestTargetSpec extends Specification {
  def 'supports any message interaction'() {
    expect:
    new MessageTestTarget().supportsInteraction(interaction) == result

    where:
    interaction                                   | result
    new RequestResponseInteraction('test')        | false
    new Message('test')                           | true
    new V4Interaction.AsynchronousMessage('test') | true
    new V4Interaction.SynchronousMessages('test') | true
    new V4Interaction.SynchronousHttp('test')     | false
  }
}
