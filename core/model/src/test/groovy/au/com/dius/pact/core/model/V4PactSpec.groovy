package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification

@SuppressWarnings('LineLength')
class V4PactSpec extends Specification {

  def 'test load v4 pact'() {
    given:
    def pactUrl = V4PactSpec.classLoader.getResource('v4-http-pact.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof V4Pact
    pact.consumer.name == 'test_consumer'
    pact.provider.name == 'test_provider'
    pact.interactions.size() == 1
    pact.interactions[0].uniqueKey() == '001'
    pact.interactions[0] instanceof V4Interaction.SynchronousHttp
    pact.interactions[0].description == 'test interaction with a binary body'
    pact.metadata['pactSpecification']['version'] == '4.0'
  }

  def 'test load v4 message pact'() {
    given:
    def pactUrl = V4PactSpec.classLoader.getResource('v4-message-pact.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof V4Pact
    pact.consumer.name == 'test_consumer'
    pact.provider.name == 'test_provider'
    pact.interactions.size() == 1
    pact.interactions[0].uniqueKey() == 'm_001'
    pact.interactions[0] instanceof V4Interaction.AsynchronousMessage
    pact.interactions[0].description == 'Test Message'
    pact.interactions[0].matchingRules.toV3Map() == [
      content: ['$.a': [matchers: [[match: 'regex', regex: '\\d+-\\d+']], combine: 'AND']]
    ]
    pact.interactions[0].generators.toMap(PactSpecVersion.V4) == [content: [a: [type: 'Uuid']]]
    pact.metadata['pactSpecification']['version'] == '4.0'
  }

  def 'test load v4 combined pact'() {
    given:
    def pactUrl = V4PactSpec.classLoader.getResource('v4-combined-pact.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof V4Pact
    pact.consumer.name == 'test_consumer'
    pact.provider.name == 'test_provider'
    pact.interactions.size() == 2
    pact.interactions[0].uniqueKey() == '001'
    pact.interactions[0] instanceof V4Interaction.SynchronousHttp
    pact.interactions[0].description == 'test interaction with a binary body'
    pact.interactions[1].uniqueKey() == 'm_001'
    pact.interactions[1] instanceof V4Interaction.AsynchronousMessage
    pact.interactions[1].description == 'Test Message'
    pact.interactions[1].matchingRules.toV3Map() == [:]
    pact.interactions[1].generators.toMap(PactSpecVersion.V4) == [content: [a: [type: 'Uuid']]]
    pact.metadata['pactSpecification']['version'] == '4.0'
  }

  def 'test load v4 pact with comments'() {
    given:
    def pactUrl = V4PactSpec.classLoader.getResource('v4-http-pact-comments.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof V4Pact
    pact.interactions.size() == 1
    pact.interactions[0].comments == [
      text: new JsonValue.Array([
        new JsonValue.StringValue('This allows me to specify just a bit more information about the interaction'.chars),
        new JsonValue.StringValue('It has no functional impact, but can be displayed in the broker HTML page, and potentially in the test output'.chars),
        new JsonValue.StringValue('It could even contain the name of the running test on the consumer side to help marry the interactions back to the test case'.chars)
      ]),
      testname: new JsonValue.StringValue('example_test.groovy'.chars)
    ]
  }

  def 'test load v4 pact with message with comments'() {
    given:
    def pactUrl = V4PactSpec.classLoader.getResource('v4-message-pact-comments.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof V4Pact
    pact.interactions.size() == 1
    pact.interactions[0].comments == [
      text: new JsonValue.Array([
        new JsonValue.StringValue('This allows me to specify just a bit more information about the interaction'.chars),
        new JsonValue.StringValue('It has no functional impact, but can be displayed in the broker HTML page, and potentially in the test output'.chars),
        new JsonValue.StringValue('It could even contain the name of the running test on the consumer side to help marry the interactions back to the test case'.chars)
      ]),
      testname: new JsonValue.StringValue('example_test.groovy'.chars)
    ]
  }

  def 'test load v4 pact with pending interactions'() {
    given:
    def pactUrl = V4PactSpec.classLoader.getResource('v4-pending-pact.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof V4Pact
    pact.interactions.size() == 2
    pact.interactions[0] instanceof V4Interaction
    pact.interactions[0].pending == true
    pact.interactions[0].toString().startsWith('Interaction: test interaction with a binary body [PENDING]')
    pact.interactions[1] instanceof V4Interaction
    pact.interactions[1].pending == true
    pact.interactions[1].toString().startsWith('Interaction: Test Message [PENDING]')
  }
}
