package au.com.dius.pact.core.model.messaging

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.InvalidPactException
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import spock.lang.Specification

class MessagePactSpec extends Specification {

  private static Provider provider
  private static Consumer consumer
  private static Message message

  def setupSpec() {
    provider = new Provider()
    consumer = new Consumer()
    message = new Message(contents: OptionalBody.body('1 2 3 4'))
  }

  def 'fails to convert the message to a Map if the target spec version is < 3'() {
    when:
    new MessagePact(provider, consumer, []).toMap(PactSpecVersion.V1)

    then:
    thrown(InvalidPactException)
  }

  @SuppressWarnings('ComparisonWithSelf')
  def 'equality test'() {
    expect:
    pact == pact

    where:
    pact = new MessagePact(provider, consumer, [ message ])
  }

  def 'pacts are not equal if the providers are different'() {
    expect:
    pact != pact2

    where:
    provider2 = new Provider('other provider')
    pact = new MessagePact(provider, consumer, [ message ])
    pact2 = new MessagePact(provider2, consumer, [ message ])
  }

  def 'pacts are not equal if the consumers are different'() {
    expect:
    pact != pact2

    where:
    consumer2 = new Consumer('other consumer')
    pact = new MessagePact(provider, consumer, [ message ])
    pact2 = new MessagePact(provider, consumer2, [ message ])
  }

  def 'pacts are equal if the metadata is different'() {
    expect:
    pact == pact2

    where:
    pact = new MessagePact(provider, consumer, [ message ], [meta: 'data'])
    pact2 = new MessagePact(provider, consumer, [ message ], [meta: 'other data'])
  }

  def 'pacts are not equal if the interactions are different'() {
    expect:
    pact != pact2

    where:
    message2 = new Message(contents: OptionalBody.body('A B C'))
    pact = new MessagePact(provider, consumer, [ message ])
    pact2 = new MessagePact(provider, consumer, [ message2 ])
  }

  def 'pacts are not equal if the number of interactions are different'() {
    expect:
    pact != pact2

    where:
    message2 = new Message(contents: OptionalBody.body('A B C'))
    pact = new MessagePact(provider, consumer, [ message ])
    pact2 = new MessagePact(provider, consumer, [ message, message2 ])
  }

  def 'when filtering the pact, do not loose the source of the pact'() {
    given:
    def source = new BrokerUrlSource('url', 'brokerUrl')
    def pact = new MessagePact(provider, consumer, [ message ])
    pact.source = source

    when:
    pact.filterInteractions { true }

    then:
    pact.source == source
  }

}
