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
      [description: 'Expected status code of 400 but got 500', interactionId: 'ABC', type: 'status']
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
        [attribute: 'status', description: 'Expected status code of 400 but got 500']
      ]
    ]
  }

  def 'mismatches should be grouped by interaction'() {
    given:
    result = new TestResult.Failed([
      [description: 'Expected status code of 400 but got 500', interactionId: 'ABC', type: 'status'],
      [description: 'Expected status code of 400 but got 500', interactionId: '123', type: 'status'],
      [description: 'Expected status code of 200 but got 500', interactionId: 'ABC', type: 'status'],
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
        [attribute: 'status', description: 'Expected status code of 400 but got 500']
      ]
    ]
    result.testResults.find { it.interactionId == 'ABC' } == [
      interactionId: 'ABC',
      description: 'Test failed',
      success: false,
      mismatches: [
        [attribute: 'status', description: 'Expected status code of 400 but got 500'],
        [attribute: 'status', description: 'Expected status code of 200 but got 500']
      ]
    ]

  }

  def 'handle body mismatches'() {
    given:
    String diff = '''
        -    "doesNotExist": "Test",
        -    "documentId": 0
        +    "tags": null,
        +    "contentLength": 0,
        +    "documentCategoryId": 5,
        +    "documentId": 1,
        +    "documentCategoryCode": null
        '''

    result = new TestResult.Failed([
      [
        comparison: [
          '$.0': [
            [
              mismatch: "Expected doesNotExist='Test' but was missing",
              diff: diff
            ]
          ],
          '$.1': [
            [
              mismatch: "Expected doesNotExist='Test' but was missing",
              diff: diff
            ]
          ],
          diff: [
            '    {',
            '-        "doesNotExist": "Test"',
            '',
            '-        "documentId": 0',
            '+        "documentId": 0',
            '',
            '+        "documentCategoryId": 5',
            '',
            '+        "documentCategoryCode": null',
            '',
            '+        "contentLength": 0',
            '',
            '+        "tags": null',
            '+    }']
        ],
        interactionId: '36803e0333e8967092c2910b9d2f75c033e696ee',
        type: 'body'
      ],
      [
        comparison: "Expected a response type of 'application/json' but the actual type was 'text/plain'",
        interactionId: '1234',
        type: 'body'
      ]
    ], 'Test failed')

    when:
    def result = buildPayload()
    def result1 = result.testResults.find { it.interactionId == '36803e0333e8967092c2910b9d2f75c033e696ee' }
    def result2 = result.testResults.find { it.interactionId == '1234' }

    then:
    result.testResults.size() == 2
    result1.description == 'Test failed'
    result1.mismatches.size() == 2
    result1.mismatches[0] == [
      attribute: 'body',
      identifier: '$.0',
      description: "Expected doesNotExist='Test' but was missing",
      diff: diff
    ]
    result1.mismatches[1] == [
      attribute: 'body',
      identifier: '$.1',
      description: "Expected doesNotExist='Test' but was missing",
      diff: diff
    ]
    result2.description == 'Test failed'
    result2.mismatches.size() == 1
    result2.mismatches[0] == [
      attribute: 'body',
      description: "Expected a response type of 'application/json' but the actual type was 'text/plain'"
    ]
  }

  def 'handle header mismatches'() {
    given:
    result = new TestResult.Failed([
      [
        identifier: 'X',
        description: "Expected header 'X' to have value '100' but was '200'",
        interactionId: '36803e0333e8967092c2910b9d2f75c033e696ee',
        type: 'header'
      ],
      [
        identifier: 'Y',
        description: "Expected header 'Y' to have value 'X' but was '100'",
        interactionId: '36803e0333e8967092c2910b9d2f75c033e696ee',
        type: 'header'
      ]
    ], 'Test failed')

    when:
    def result = buildPayload()

    then:
    result.testResults.size() == 1
    result.testResults[0] == [
      interactionId: '36803e0333e8967092c2910b9d2f75c033e696ee',
      success: false,
      description: 'Test failed',
      mismatches: [
        [attribute: 'header', identifier: 'X', description: "Expected header 'X' to have value '100' but was '200'"],
        [attribute: 'header', identifier: 'Y', description: "Expected header 'Y' to have value 'X' but was '100'"]
      ]
    ]
  }
}
