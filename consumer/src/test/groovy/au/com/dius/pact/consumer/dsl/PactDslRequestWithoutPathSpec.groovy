package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import spock.lang.Issue
import spock.lang.Specification

class PactDslRequestWithoutPathSpec extends Specification {

  def 'sets up any default state when created'() {
    given:
    ConsumerPactBuilder consumerPactBuilder = ConsumerPactBuilder.consumer('spec')
    PactDslWithState pactDslWithState = new PactDslWithState(consumerPactBuilder, 'spec', 'spec', null, null)
    PactDslRequestWithoutPath defaultRequestValues = new PactDslRequestWithoutPath(consumerPactBuilder,
      pactDslWithState, 'test', null, null, [:])
      .method('PATCH')
      .headers('test', 'test')
      .query('test=true')
      .body('{"test":true}')

    when:
    PactDslRequestWithoutPath subject = new PactDslRequestWithoutPath(consumerPactBuilder, pactDslWithState, 'test',
      defaultRequestValues, null, [:])

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
      pactDslWithState, 'test', null, null, [:])
      .headers('content-type', 'text/plain')
      .body(new PactDslJsonBody())

    then:
    request.requestHeaders == ['content-type': ['text/plain']]
  }

  def 'allows setting any additional metadata'() {
    given:
    ConsumerPactBuilder consumerPactBuilder = ConsumerPactBuilder.consumer('spec')
    PactDslWithState pactDslWithState = new PactDslWithState(consumerPactBuilder, 'spec', 'spec', null, null)
    PactDslRequestWithoutPath subject = new PactDslRequestWithoutPath(consumerPactBuilder, pactDslWithState, 'test',
      null, null, [:])

    when:
    subject.addMetadataValue('test', 'value')

    then:
    subject.additionalMetadata == [test: 'value']
  }

  @Issue('#1623')
  def 'supports setting a content type matcher'() {
    given:
    def request = ConsumerPactBuilder.consumer('spec')
      .hasPactWith('provider')
      .uponReceiving('a XML request')
    def example = '<?xml version=\"1.0\" encoding=\"utf-8\"?><example>foo</example>'

    when:
    def result = request.bodyMatchingContentType('application/xml', example)

    then:
    result.requestHeaders['Content-Type'] == ['application/xml']
    result.requestBody.valueAsString() == example
    result.requestMatchers.rulesForCategory('body').toMap(PactSpecVersion.V4) == [
      '$': [matchers: [[match: 'contentType', value: 'application/xml']], combine: 'AND']
    ]
  }

  @Issue('#1767')
  def 'match path should valid the example against the regex'() {
    given:
    def request = ConsumerPactBuilder.consumer('spec')
      .hasPactWith('provider')
      .uponReceiving('a XML request')

    when:
    request.matchPath('\\/\\d+', '/abcd')

    then:
    def ex = thrown(au.com.dius.pact.consumer.InvalidMatcherException)
    ex.message == 'Example "/abcd" does not match regular expression "\\/\\d+"'
  }

  @Issue('#1777')
  def 'supports setting binary body contents'() {
    given:
    def request = ConsumerPactBuilder.consumer('spec')
      .hasPactWith('provider')
      .uponReceiving('a PUT request with binary data')
    def gif1px = [
      0107, 0111, 0106, 0070, 0067, 0141, 0001, 0000, 0001, 0000, 0200, 0000, 0000, 0377, 0377, 0377,
      0377, 0377, 0377, 0054, 0000, 0000, 0000, 0000, 0001, 0000, 0001, 0000, 0000, 0002, 0002, 0104,
      0001, 0000, 0073
    ] as byte[]

    when:
    def result = request.withBinaryData(gif1px, 'image/gif')

    then:
    result.requestHeaders['Content-Type'] == ['image/gif']
    result.requestBody.value == gif1px
    result.requestMatchers.rulesForCategory('body').toMap(PactSpecVersion.V4) == [
      '$': [matchers: [[match: 'contentType', value: 'image/gif']], combine: 'AND']
    ]
  }

  @Issue('#1826')
  def 'matchPath handles regular expressions with anchors'() {
    given:
    def request = ConsumerPactBuilder.consumer('spec')
      .hasPactWith('provider')
      .uponReceiving('request with a regex path')

    when:
    def result = request.matchPath('/pet/[0-9]+$')

    then:
    result.path ==~ /\/pet\/[0-9]+/
  }
}
