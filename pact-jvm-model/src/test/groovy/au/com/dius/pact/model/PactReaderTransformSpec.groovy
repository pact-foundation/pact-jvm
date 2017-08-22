package au.com.dius.pact.model

import groovy.json.JsonSlurper
import spock.lang.Specification

class PactReaderTransformSpec extends Specification {
  private provider
  private consumer
  private jsonMap
  private request
  private Map<String, Serializable> response

  def setup() {
    provider = [
      name: 'Alice Service'
    ]
    consumer = [
      name: 'Consumer'
    ]
    request = [
      method: 'GET',
      path: '/mallory',
      query: 'name=ron&status=good',
      body: [
        'id': '123', 'method': 'create'
      ]
    ]
    response = [
      status: 200,
      headers: [
        'Content-Type': 'text/html'
      ],
      body: '"That is some good Mallory."'
    ]

    jsonMap = new JsonSlurper().parse(this.class.getResourceAsStream('/pact.json'))
  }

  def 'only transforms legacy fields'() {
    when:
    def result = PactReader.transformJson(jsonMap)

    then:
    result == [
      provider: provider,
      consumer: consumer,
      interactions: [
        [
          description: 'a retrieve Mallory request',
          request: request,
          response: response
        ]
      ]
    ]
  }

  def 'converts provider state to camel case'() {
    given:
    jsonMap.interactions[0].provider_state = 'provider state'

    when:
    def result = PactReader.transformJson(jsonMap)

    then:
    result == [
      provider: provider,
      consumer: consumer,
      interactions: [
        [
          description: 'a retrieve Mallory request',
          providerState: 'provider state',
          request: request,
          response: response
        ]
      ]
    ]
  }

  def 'handles both a snake and camel case provider state'() {
    given:
    jsonMap.interactions[0].provider_state = 'provider state'
    jsonMap.interactions[0].providerState = 'provider state 2'

    when:
    def result = PactReader.transformJson(jsonMap)

    then:
    result == [
      provider: provider,
      consumer: consumer,
      interactions: [
        [
          description: 'a retrieve Mallory request',
          providerState: 'provider state 2',
          request: request,
          response: response
        ]
      ]
    ]
  }

  def 'converts request and response matching rules'() {
    given:
    jsonMap.interactions[0].request.requestMatchingRules = [body: ['$': [['match': 'type']]]]
    jsonMap.interactions[0].response.responseMatchingRules = [body: ['$': [['match': 'type']]]]

    when:
    def result = PactReader.transformJson(jsonMap)

    then:
    result == [
      provider: provider,
      consumer: consumer,
      interactions: [
        [
          description: 'a retrieve Mallory request',
          request: request + [matchingRules: [body: ['$': [ [match: 'type'] ]]]],
          response: response + [matchingRules: [body: ['$': [ [match: 'type']]]]]
        ]
      ]
    ]
  }

  def 'converts the http methods to upper case'() {
    given:
    jsonMap.interactions[0].request.method = 'get'

    when:
    def result = PactReader.transformJson(jsonMap)

    then:
    result == [
      provider: provider,
      consumer: consumer,
      interactions: [
        [
          description: 'a retrieve Mallory request',
          request: request,
          response: response
        ]
      ]
    ]
  }

}
