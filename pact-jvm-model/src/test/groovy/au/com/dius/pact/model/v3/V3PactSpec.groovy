package au.com.dius.pact.model.v3

import au.com.dius.pact.model.InvalidPactException
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import spock.lang.Specification

class V3PactSpec extends Specification {
    private File pactFile

    def setup() {
      pactFile = File.createTempFile('message-pact', '.json')
      def pactUrl = V3PactSpec.classLoader.getResource('v3-message-pact.json')
      pactFile.write(pactUrl.text)
    }

    def cleanup() {
      pactFile.delete()
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
                    ],
                    metadata: metadata
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
                    ],
                    metadata: metadata
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
    }

    def 'refuse to merge pacts with different spec versions'() {
        given:
        def json = new JsonSlurper().parse(pactFile)
        json.metadata['pact-specification'].version = '2.0.0'
        pactFile.write(new JsonBuilder(json).toPrettyString())

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
                  ],
                  metadata: metadata
                ]
            }

            @SuppressWarnings('UnusedMethodParameter')
            protected File fileForPact(String pactDir) {
                pactFile
            }
        }

        when:
        pact.write('/some/pact/dir')

        then:
        InvalidPactException e = thrown()
        e.message.contains('pact specification version is 3.0.0, while the file is version 2.0.0')
    }

    def 'refuse to merge pacts with different types (message vs request-response)'() {
        given:
        def pactUrl = V3PactSpec.classLoader.getResource('v3-pact.json')
        pactFile.write(pactUrl.text)

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
                  ],
                  metadata: metadata
                ]
            }

            @SuppressWarnings('UnusedMethodParameter')
            protected File fileForPact(String pactDir) {
                pactFile
            }
        }

        when:
        pact.write('/some/pact/dir')

        then:
        InvalidPactException e = thrown()
        e.message.contains('file is not a message pact (it contains request/response interactions)')
    }

}
