package au.com.dius.pact.provider

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.messaging.Message
import spock.lang.Specification

class MessageComparisonSpec extends Specification {

  def 'compares the message contents as JSON by default'() {
    given:
    def message = new Message(contents: OptionalBody.body('{"a":1,"b":"2"}'.bytes))
    def actual = OptionalBody.body('{"a":1,"b":"3"}'.bytes)

    when:
    def result = ResponseComparison.compareMessage(message, actual)

    then:
    result.body.comparison == [
      '$.b': [[mismatch: 'Expected "2" but received "3"', diff: '']]
    ]
  }

  def 'compares the message contents by the content type'() {
    given:
    def message = new Message(contents: OptionalBody.body('{"a":1,"b":"2"}'.bytes),
      metaData: [contentType: 'text/plain'])
    def actual = OptionalBody.body('{"a":1,"b":"3"}'.bytes)

    when:
    def result = ResponseComparison.compareMessage(message, actual)

    then:
    result.body.comparison == [
      '/': [
        [
          mismatch: "Expected body '{\"a\":1,\"b\":\"2\"}' to match '{\"a\":1,\"b\":\"3\"}' using equality but did " +
            'not match',
          diff: ''
        ]
      ]
    ]
  }

  def 'compares the metadata if provided'() {
    given:
    def message = new Message(contents: OptionalBody.body('{"a":1,"b":"2"}'.bytes), metaData: [
      contentType: 'application/json',
      destination: 'X001'
    ])
    def actual = OptionalBody.body('{"a":1,"b":"2"}'.bytes)
    def actualMetadata = [destination: 'X002']

    when:
    def result = ResponseComparison.compareMessage(message, actual, actualMetadata)

    then:
    result.metadata == [
      'destination': "Expected metadata key 'destination' to have value 'X001' (String) but was 'X002' (String)"
    ]
  }

}
