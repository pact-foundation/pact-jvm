package au.com.dius.pact.provider.broker

import au.com.dius.pact.pactbroker.TestResult
import groovy.json.JsonSlurper
import spock.lang.Specification

class VerificationResultPayloadSpec extends Specification {

  private PactBrokerClient pactBrokerClient
  private TestResult result
  private String version
  private String buildUrl

  def setup() {
    pactBrokerClient = new PactBrokerClient('http://localhost:8080')
    version = '0.0.0'
  }

  Map buildPayload() {
    new JsonSlurper().parseText(pactBrokerClient.buildPayload(result, version, buildUrl).toString()) as Map
  }

  def 'exceptions should be serialised as a message and exception class'() {
    given:
    result = new TestResult.Failed([
      [message: 'test failed', 'exception': new IOException('Boom'), interactionId: 'ABC'],
      [description: 'Expected status code of 400 but got 500', interactionId: 'ABC']
    ], 'Test failed with exception')

    when:
    def result = buildPayload()

    then:
    result.testResults.size() == 1
    result.testResults.first() == [
      interactionId: 'ABC',
      description: 'Test failed with exception',
      success: false,
      exception: [
        message: 'Boom',
        exceptionClass: 'java.io.IOException'
      ],
      mismatches: [
        [description: 'Expected status code of 400 but got 500']
      ]
    ]
  }

  def 'mismatches should be grouped by interaction'() {
    given:
    result = new TestResult.Failed([
      [description: 'Expected status code of 400 but got 500', interactionId: 'ABC'],
      [description: 'Expected status code of 400 but got 500', interactionId: '123'],
      [description: 'Expected status code of 200 but got 500', interactionId: 'ABC'],
      [message: 'test failed', 'exception': new IOException('Boom'), interactionId: '123']
    ], 'Test failed')

    when:
    def result = buildPayload()

    then:
    result.testResults.size() == 2
    result.testResults.find { it.interactionId == '123' } == [
      interactionId: '123',
      description: 'Test failed',
      success: false,
      exception: [
        message: 'Boom',
        exceptionClass: 'java.io.IOException'
      ],
      mismatches: [
        [description: 'Expected status code of 400 but got 500']
      ]
    ]
    result.testResults.find { it.interactionId == 'ABC' } == [
      interactionId: 'ABC',
      description: 'Test failed',
      success: false,
      mismatches: [
        [description: 'Expected status code of 400 but got 500'],
        [description: 'Expected status code of 200 but got 500']
      ]
    ]

  }

}
