package au.com.dius.pact.consumer.groovy.messaging

import groovy.json.JsonSlurper
import spock.lang.Ignore
import spock.lang.Specification

class PactMessageBuilderSpec extends Specification {

    def builder = new PactMessageBuilder()

    def setup() {
        builder {
            serviceConsumer 'MessageConsumer'
            hasPactWith 'MessageProvider'
        }
    }

    @Ignore
    def 'allows matching on a message'() {
        given:
        builder {
            given 'the provider has data for a message'
            expectsToReceive 'a confirmation message for a group order'
            withMetaData(contentType: 'application/json')
            withContent {
                name 'Bob'
                date '2000-01-01'
                status 'bad'
                age 100
            }
        }

        when:
        def result = builder.run { message ->
            def content = new JsonSlurper().parse(message.contentAsBytes())
            assert content.name == 'Bob'
            assert content.date == '2000-01-01'
            assert content.status == 'bad'
            assert content.age == 100
        }

        then:
        true
    }

}
