package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.core.model.OptionalBody
import spock.lang.Issue
import spock.lang.Specification

class PactDslRequestWithoutPathSpec extends Specification {

  def 'sets up any default state when created'() {
    given:
    ConsumerPactBuilder consumerPactBuilder = ConsumerPactBuilder.consumer('spec')
    PactDslWithState pactDslWithState = new PactDslWithState(consumerPactBuilder, 'spec', 'spec', null, null)
    PactDslRequestWithoutPath defaultRequestValues = new PactDslRequestWithoutPath(consumerPactBuilder,
      pactDslWithState, 'test', null, null)
      .method('PATCH')
      .headers('test', 'test')
      .query('test=true')
      .body('{"test":true}')

    when:
    PactDslRequestWithoutPath subject = new PactDslRequestWithoutPath(consumerPactBuilder, pactDslWithState, 'test',
      defaultRequestValues, null)

    then:
    subject.requestMethod == 'PATCH'
    subject.requestHeaders == [test: ['test']]
    subject.query == [test: ['true']]
    subject.requestBody == OptionalBody.body('{"test":true}'.bytes)
  }

  @Issue('#1121')
  def 'content type header is case sensitive'() {
    given:
    ConsumerPactBuilder consumerPactBuilder = ConsumerPactBuilder.consumer('spec')
    PactDslWithState pactDslWithState = new PactDslWithState(consumerPactBuilder, 'spec', 'spec', null, null)

    when:
    PactDslRequestWithoutPath request = new PactDslRequestWithoutPath(consumerPactBuilder,
      pactDslWithState, 'test', null, null)
      .headers('content-type', 'text/plain')
      .body(new PactDslJsonBody())

    then:
    request.requestHeaders == ['content-type': ['text/plain']]
  }

}
