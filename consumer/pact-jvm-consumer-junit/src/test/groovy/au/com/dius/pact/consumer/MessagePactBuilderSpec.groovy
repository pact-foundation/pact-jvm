package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.core.model.messaging.Message
import spock.lang.Specification

class MessagePactBuilderSpec extends Specification {

  def 'builder should close the DSL objects correctly'() {
    given:
    PactDslJsonBody getBody = new PactDslJsonBody()
    getBody
      .object('metadata')
      .stringType('messageId', 'test')
      .stringType('date', 'test')
      .stringType('contractVersion', 'test')
      .closeObject()
      .object('payload')
      .stringType('name', 'srm.countries.get')
      .stringType('iri', 'some_iri')
      .closeObject()
      .closeObject()

    MessagePactBuilder builder = new MessagePactBuilder('MessagePactBuilderSpec')
    builder.given('srm.countries.get_message')
      .expectsToReceive('srm.countries.get')
      .withContent(getBody)

    when:
    def pact = builder.toPact()
    Message message = pact.interactions.first()

    then:
    message.matchingRules.rules.body.matchingRules.keySet() == [
      '$.metadata.messageId', '$.metadata.date', '$.metadata.contractVersion', '$.payload.name', '$.payload.iri'
    ] as Set
  }

}
