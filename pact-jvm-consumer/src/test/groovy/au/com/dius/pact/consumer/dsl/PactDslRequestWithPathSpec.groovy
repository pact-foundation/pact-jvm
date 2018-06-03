package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.generators.Generators
import au.com.dius.pact.model.matchingrules.MatchingRulesImpl
import spock.lang.Specification

class PactDslRequestWithPathSpec extends Specification {

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
    PactDslRequestWithPath subject = new PactDslRequestWithPath(consumerPactBuilder, 'spec', 'spec', [], 'test', '/',
      'GET', [:], [:], OptionalBody.empty(), new MatchingRulesImpl(), new Generators(), defaultRequestValues, null)
    PactDslRequestWithPath subject2 = new PactDslRequestWithPath(consumerPactBuilder, subject, 'test',
      defaultRequestValues, null)

    then:
    subject.requestMethod == 'PATCH'
    subject.requestHeaders == [test: 'test']
    subject.query == [test: ['true']]
    subject.requestBody == OptionalBody.body('{"test":true}')

    subject2.requestMethod == 'PATCH'
    subject2.requestHeaders == [test: 'test']
    subject2.query == [test: ['true']]
    subject2.requestBody == OptionalBody.body('{"test":true}')
  }

}
