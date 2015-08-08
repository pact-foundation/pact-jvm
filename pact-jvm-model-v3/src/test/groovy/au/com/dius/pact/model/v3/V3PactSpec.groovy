package au.com.dius.pact.model.v3

import groovy.json.JsonSlurper
import spock.lang.Specification

class V3PactSpec extends Specification {
    private File pactFile

    def setup() {
        pactFile = File.createTempFile('message-pact', '.json')
        def pactUrl = V3PactSpec.classLoader.getResource('v3-message-pact.json')
        pactFile.write(pactUrl.text)
    }

    def 'writing pacts should merge with any existing file'() {
        given:
        def pact = new V3Pact() {
            @Override
            Map toMap() {
                [
                    consumer: [name: 'asis-trading-order-repository'],
                    provider: [name: 'asis-core'],
                    messages: [
                        [
                            providerState: 'a new message exists',
                            contents: 'Hello',
                            description: 'a new hello message'
                        ]
                    ]
                ]
            }

            @SuppressWarnings('UnusedMethodParameter')
            protected File fileForPact(String pactDir) {
                pactFile
            }
        }

        when:
        pact.write('/some/pact/dir')
        def json = new JsonSlurper().parse(pactFile)

        then:
        json.messages.size == 2
        json.messages*.description.toSet() == ['a hello message', 'a new hello message'].toSet()

        cleanup:
        pactFile.delete()
    }

    def 'when merging it should replace messages with the same description'() {
        given:
        def pact = new V3Pact() {
            @Override
            Map toMap() {
                [
                    consumer: [name: 'asis-trading-order-repository'],
                    provider: [name: 'asis-core'],
                    messages: [
                        [
                            providerState: 'a new message exists',
                            contents: 'Hello',
                            description: 'a new hello message'
                        ], [
                            contents: 'Hello',
                            description: 'a hello message'
                        ]
                    ]
                ]
            }

            @SuppressWarnings('UnusedMethodParameter')
            protected File fileForPact(String pactDir) {
                pactFile
            }
        }

        when:
        pact.write('/some/pact/dir')
        def json = new JsonSlurper().parse(pactFile)

        then:
        json.messages.size == 2
        json.messages*.description.toSet() == ['a hello message', 'a new hello message'].toSet()
        json.messages.find { it.description == 'a hello message' } == [contents: 'Hello',
            description: 'a hello message']

        cleanup:
        pactFile.delete()
    }

}
