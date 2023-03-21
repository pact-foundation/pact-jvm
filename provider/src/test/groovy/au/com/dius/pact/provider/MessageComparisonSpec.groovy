package au.com.dius.pact.provider

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.support.Result
import spock.lang.Specification

@SuppressWarnings('LineLength')
class MessageComparisonSpec extends Specification {

  def responseComparison = ResponseComparison.Companion.newInstance()

  def 'compares the message contents as JSON'() {
    given:
    def message = new Message('test', [], OptionalBody.body('{"a":1,"b":"2"}'.bytes))
    def actual = OptionalBody.body('{"a":1,"b":"3"}'.bytes)

    when:
    def result = responseComparison.compareMessage(message, actual, null, [:]).bodyMismatches

    then:
    result instanceof Result.Ok
    result.value.mismatches.collectEntries { [ it.key, it.value*.description() ] } == [
      '$.b': ['Expected \'2\' (String) but received \'3\' (String)']
    ]
  }

  def 'compares the message contents by the content type'() {
    given:
    def message = new Message('test', [], OptionalBody.body('{"a":1,"b":"2"}'.bytes), new MatchingRulesImpl(),
      new Generators(), [contentType: 'text/plain'])
    def actual = OptionalBody.body('{"a":1,"b":"3"}'.bytes)

    when:
    def result = responseComparison.compareMessage(message, actual, null, [:]).bodyMismatches

    then:
    result instanceof Result.Ok
    result.value.mismatches.collectEntries { [ it.key, it.value*.description() ] } == [
      '/': [
        'Expected body \'{"a":1,"b":"2"}\' to match \'{"a":1,"b":"3"}\' using equality but did not match'
      ]
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
    def result = responseComparison.compareMessage(message, actual, actualMetadata, [:]).metadataMismatches.collectEntries {
      [ it.key, it.value*.description() ]
    }

    then:
    result == [
      'destination': [
        "Expected metadata key 'destination' to have value 'X001' (String) but was 'X002' (String)"
      ]
    ]
  }
}
