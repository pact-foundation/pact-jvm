package au.com.dius.pact.core.model

import spock.lang.Specification

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
}
