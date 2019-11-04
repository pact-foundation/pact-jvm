package au.com.dius.pact.provider

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.messaging.Message
import spock.lang.Specification

class MessageComparisonSpec extends Specification {

  def 'compares the message contents as JSON by default'() {
    given:
    def message = new Message('test', [], OptionalBody.body('{"a":1,"b":"2"}'.bytes))
    def actual = OptionalBody.body('{"a":1,"b":"3"}'.bytes)

    when:
    def result = ResponseComparison.compareMessage(message, actual).bodyMismatches

    then:
    result.isRight()
    result.b.mismatches.collectEntries { [ it.key, it.value*.description() ] } == [
      '$.b': ['Expected "2" but received "3"']
    ]
  }

  def 'compares the message contents by the content type'() {
    given:
    def message = new Message('test', [], OptionalBody.body('{"a":1,"b":"2"}'.bytes), new MatchingRulesImpl(),
      new Generators(), [contentType: 'text/plain'])
    def actual = OptionalBody.body('{"a":1,"b":"3"}'.bytes)

    when:
    def result = ResponseComparison.compareMessage(message, actual).bodyMismatches

    then:
    result.isRight()
    result.b.mismatches.collectEntries { [ it.key, it.value*.description() ] } == [
      '/': ["Expected body '{\"a\":1,\"b\":\"2\"}' to match '{\"a\":1,\"b\":\"3\"}' using equality but did " +
            'not match']
    ]
  }

  def 'compares the metadata if provided'() {
    given:
    def message = new Message('test', [], OptionalBody.body('{"a":1,"b":"2"}'.bytes), new MatchingRulesImpl(),
      new Generators(), [
      contentType: 'application/json',
      destination: 'X001'
    ])
    def actual = OptionalBody.body('{"a":1,"b":"2"}'.bytes)
    def actualMetadata = [destination: 'X002']

    when:
    def result = ResponseComparison.compareMessage(message, actual, actualMetadata).metadataMismatches.collectEntries {
      [ it.key, it.value*.description() ]
    }

    then:
    result == [
      'destination': ["Expected metadata key 'destination' to have value 'X001' (String) but was 'X002' (String)"]
    ]
  }

}
