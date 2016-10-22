package au.com.dius.pact.model.v3.messaging

import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.InvalidPactException
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.Provider
import spock.lang.Specification

class MessagePactSpec extends Specification {

  def 'fails to convert the message to a Map if the target spec version is < 3'() {
    when:
    new MessagePact(new Provider(), new Consumer(), []).toMap(PactSpecVersion.V1)

    then:
    thrown(InvalidPactException)
  }

  @SuppressWarnings('ComparisonWithSelf')
  def 'equality test'() {
    expect:
    pact == pact

    where:
    provider = new Provider()
    consumer = new Consumer()
    message = new Message(contents: OptionalBody.body('1 2 3 4'))
    pact = new MessagePact(provider, consumer, [ message ])
  }

  def 'pacts are not equal if the providers are different'() {
    expect:
    pact != pact2

    where:
    provider = new Provider()
    provider2 = new Provider('other provider')
    consumer = new Consumer()
    message = new Message(contents: OptionalBody.body('1 2 3 4'))
    pact = new MessagePact(provider, consumer, [ message ])
    pact2 = new MessagePact(provider2, consumer, [ message ])
  }

  def 'pacts are not equal if the consumers are different'() {
    expect:
    pact != pact2

    where:
    provider = new Provider()
    consumer = new Consumer()
    consumer2 = new Consumer('other consumer')
    message = new Message(contents: OptionalBody.body('1 2 3 4'))
    pact = new MessagePact(provider, consumer, [ message ])
    pact2 = new MessagePact(provider, consumer2, [ message ])
  }

  def 'pacts are equal if the metadata is different'() {
    expect:
    pact == pact2

    where:
    provider = new Provider()
    consumer = new Consumer()
    message = new Message(contents: OptionalBody.body('1 2 3 4'))
    pact = new MessagePact(provider, consumer, [ message ], [meta: 'data'])
    pact2 = new MessagePact(provider, consumer, [ message ], [meta: 'other data'])
  }

  def 'pacts are not equal if the interactions are different'() {
    expect:
    pact != pact2

    where:
    provider = new Provider()
    consumer = new Consumer()
    message = new Message(contents: OptionalBody.body('1 2 3 4'))
    message2 = new Message(contents: OptionalBody.body('A B C'))
    pact = new MessagePact(provider, consumer, [ message ])
    pact2 = new MessagePact(provider, consumer, [ message2 ])
  }

  def 'pacts are not equal if the number of interactions are different'() {
    expect:
    pact != pact2

    where:
    provider = new Provider()
    consumer = new Consumer()
    message = new Message(contents: OptionalBody.body('1 2 3 4'))
    message2 = new Message(contents: OptionalBody.body('A B C'))
    pact = new MessagePact(provider, consumer, [ message ])
    pact2 = new MessagePact(provider, consumer, [ message, message2 ])
  }

}
