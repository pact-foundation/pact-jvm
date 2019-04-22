package au.com.dius.pact.consumer.groovy.messaging

import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.messaging.Message
import groovy.json.JsonSlurper
import spock.lang.Specification

class PactMessageBuilderSpec extends Specification {

  def builder = new PactMessageBuilder()

  def setup() {
    builder {
      serviceConsumer 'MessageConsumer'
      hasPactWith 'MessageProvider'
    }
  }

  def 'allows receiving a message'() {
    given:
    builder {
      given 'the provider has data for a message'
      expectsToReceive 'a confirmation message for a group order'
      withMetaData(contentType: 'application/json')
      withContent {
        name 'Bob'
        date = '2000-01-01'
        status 'bad'
        age 100
      }
    }

    when:
    builder.run { Message message ->
      def content = new JsonSlurper().parse(message.contentsAsBytes())
      assert content.name == 'Bob'
      assert content.date == '2000-01-01'
      assert content.status == 'bad'
      assert content.age == 100
    }

    then:
    true
  }

  def 'by default pretty prints bodies'() {
    given:
    builder {
      given 'the provider has data for a message'
      expectsToReceive 'a confirmation message for a group order'
      withMetaData(contentType: 'application/json')
      withContent {
        name 'Bob'
        date = '2000-01-01'
        status 'bad'
        age 100
      }
    }

    when:
    def message = builder.messages.first()

    then:
    new String(message.contentsAsBytes()) == '''|{
                                                |    "name": "Bob",
                                                |    "date": "2000-01-01",
                                                |    "status": "bad",
                                                |    "age": 100
                                                |}'''.stripMargin()
  }

  def 'allows turning off pretty printed bodies'() {
    given:
    builder {
      given 'the provider has data for a message'
      expectsToReceive 'a confirmation message for a group order'
      withMetaData(contentType: 'application/json')
      withContent(prettyPrint: false) {
        name 'Bob'
        date = '2000-01-01'
        status 'bad'
        age 100
      }
    }

    when:
    def message = builder.messages.first()

    then:
    new String(message.contentsAsBytes()) == '{"name":"Bob","date":"2000-01-01","status":"bad","age":100}'
  }

  def 'allows using matchers with the metadata'() {
    given:
    builder {
      given 'the provider has data for a message'
      expectsToReceive 'a confirmation message for a group order'
      withMetaData(contentType: 'application/json', destination: regexp(~/X\d+/, 'X01'))
      withContent {
        name 'Bob'
        date = '2000-01-01'
        status 'bad'
        age 100
      }
    }

    when:
    builder.run { Message message ->
      assert message.metaData == [contentType: 'application/json', destination: 'X01']
      assert message.matchingRules.rules.metadata.matchingRules == [
        destination: new MatchingRuleGroup([new RegexMatcher('X\\d+', 'X01')])
      ]
    }

    then:
    true
  }

}
