package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.messaging.Message
import spock.lang.Specification

class HttpTestTargetSpec extends Specification {
  def 'supports any HTTP interaction'() {
    expect:
    new HttpTestTarget().supportsInteraction(interaction) == result

    where:
    interaction                                   | result
    new RequestResponseInteraction('test')        | true
    new Message('test')                           | false
    new V4Interaction.AsynchronousMessage('test') | false
    new V4Interaction.SynchronousMessages('test') | false
    new V4Interaction.SynchronousHttp('test')     | true
  }
}
