package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import spock.lang.Issue
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
      'GET', [:], [:], OptionalBody.empty(), new MatchingRulesImpl(), new Generators(), defaultRequestValues, null, [])
    PactDslRequestWithPath subject2 = new PactDslRequestWithPath(consumerPactBuilder, subject, 'test',
      defaultRequestValues, null)

    then:
    subject.requestMethod == 'PATCH'
    subject.requestHeaders == [test: ['test']]
    subject.query == [test: ['true']]
    subject.requestBody == OptionalBody.body('{"test":true}'.bytes)

    subject2.requestMethod == 'PATCH'
    subject2.requestHeaders == [test: ['test']]
    subject2.query == [test: ['true']]
    subject2.requestBody == OptionalBody.body('{"test":true}'.bytes)
  }

  @Issue('#716')
  def 'set the content type header correctly (issue #716)'() {
    given:
    def builder = ConsumerPactBuilder.consumer('spec').hasPactWith('provider')
    def body = new PactDslJsonBody().numberValue('key', 1).close()

    when:
    def pact = builder
      .given('Given the body method is invoked before the header method')
      .uponReceiving('a request for some response')
      .path('/bad/content-type/matcher')
      .method('GET')
      .body(body)
      .matchHeader('Content-Type', 'application/json')
      .willRespondWith()
      .status(200)

      .given('Given the body method is invoked after the header method')
      .uponReceiving('a request for some response')
      .path('/no/content-type/matcher')
      .method('GET')
      .matchHeader('Content-Type', 'application/json')
      .body(body)
      .willRespondWith()
      .status(200)
      .toPact()

    def requests = pact.interactions*.request

    then:
    requests[0].matchingRules.rulesForCategory('header').matchingRules['Content-Type'].rules == [
      new RegexMatcher('application/json')
    ]
    requests[1].matchingRules.rulesForCategory('header').matchingRules['Content-Type'].rules == [
      new RegexMatcher('application/json')
    ]
  }

  @Issue('#883')
  @Issue('#1435')
  def 'Pact with PactDslRootValue as body'() {
    given:
    def builder = ConsumerPactBuilder.consumer('spec').hasPactWith('provider')
    def body = PactDslRootValue.stringType('example')

    when:
    def pact = builder
      .given('Given a body that is a string')
        .uponReceiving('a request for a string')
        .path('/string')
        .method('POST')
        .body(body)
      .willRespondWith()
        .status(200)
        .body(body)
        .toPact()

    def request = pact.interactions[0].request
    def response = pact.interactions[0].response

    then:
    request.body.valueAsString() == 'example'
    request.matchingRules.rulesForCategory('body').matchingRules['$'].rules*.class.simpleName == [
      'TypeMatcher']
    response.body.valueAsString() == 'example'
    response.matchingRules.rulesForCategory('body').matchingRules['$'].rules*.class.simpleName == [
      'TypeMatcher']
  }

  @Issue('#883')
  @Issue('#1435')
  def 'Pact with PactDslJsonRootValue as body'() {
    given:
    def builder = ConsumerPactBuilder.consumer('spec').hasPactWith('provider')
    def body = PactDslJsonRootValue.stringType('example')

    when:
    def pact = builder
      .given('Given a body that is a string')
      .uponReceiving('a request for a string')
      .path('/string')
      .method('POST')
      .body(body)
      .willRespondWith()
      .status(200)
      .body(body)
      .toPact()

    def request = pact.interactions[0].request
    def response = pact.interactions[0].response

    then:
    request.body.valueAsString() == '"example"'
    request.matchingRules.rulesForCategory('body').matchingRules['$'].rules*.class.simpleName == [
      'TypeMatcher']
    response.body.valueAsString() == '"example"'
    response.matchingRules.rulesForCategory('body').matchingRules['$'].rules*.class.simpleName == [
      'TypeMatcher']

  }

  @Issue('#1018')
  def 'Request query gets mangled/encoded '() {
    given:
    def builder = ConsumerPactBuilder.consumer('spec').hasPactWith('provider')

    when:
    def pact = builder
      .uponReceiving('a request with query parameters')
      .path('/')
      .query('include[]=term&include[]=total_scores&include[]=license&include[]=is_public&include[]=needs_' +
        'grading_count&include[]=permissions&include[]=current_grading_period_scores&include[]=course_image&' +
        'include[]=favorites')
      .willRespondWith()
      .status(200)
      .toPact()

    def request = pact.interactions[0].request

    then:
    request.query == [
      'include[]': ['term', 'total_scores', 'license', 'is_public', 'needs_grading_count', 'permissions',
                    'current_grading_period_scores', 'course_image', 'favorites']
    ]
  }

  @Issue('#1121')
  def 'content type header is case sensitive'() {
    given:
    ConsumerPactBuilder consumerPactBuilder = ConsumerPactBuilder.consumer('spec')

    when:
    PactDslRequestWithPath request = new PactDslRequestWithPath(consumerPactBuilder,
      'test', 'test', [], 'test', '/', 'GET', [:], [:], OptionalBody.missing(), new MatchingRulesImpl(),
      new Generators(), null, null, [])
      .headers('content-type', 'text/plain')
      .body(new PactDslJsonBody())

    then:
    request.requestHeaders == ['content-type': ['text/plain']]
  }
}
