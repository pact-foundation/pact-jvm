package au.com.dius.pact.model.v3

import au.com.dius.pact.model.BasePact
import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.InvalidPactException
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.PactReader
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.Provider
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import spock.lang.Specification

class V3PactSpec extends Specification {
    private File pactFile

    def setup() {
      pactFile = new File(File.createTempDir(), 'consumer-provider.json')
      def pactUrl = V3PactSpec.classLoader.getResource('v3-message-pact.json')
      pactFile.write(pactUrl.text)
    }

    def cleanup() {
      pactFile.delete()
    }

    def 'writing pacts should merge with any existing file'() {
        given:
        def pact = PactReader.loadV3Pact(null, [
          consumer: [name: 'consumer'],
          provider: [name: 'provider'],
          messages: [
            [
              providerStates: [[name: 'a new message exists']],
              contents: 'Hello',
              description: 'a new hello message',
              metaData: [ contentType: 'application/json' ]
            ]
          ],
          metadata: BasePact.DEFAULT_METADATA
        ])

        when:
        pact.write(pactFile.parentFile.toString(), PactSpecVersion.V3)
        def json = new JsonSlurper().parse(pactFile)

        then:
        json.messages.size == 2
        json.messages*.description.toSet() == ['a hello message', 'a new hello message'].toSet()
    }

    def 'when merging it should replace messages with the same description and state'() {
        given:
        def pact = PactReader.loadV3Pact(null, [
            consumer: [name: 'consumer'],
            provider: [name: 'provider'],
            messages: [
              [
                providerStates: [[name: 'message exists']],
                contents: 'Hello',
                description: 'a hello message',
                metaData: [ contentType: 'application/json' ]
              ], [
                  providerStates: [[name: 'a new message exists']],
                  contents: 'Hello',
                  description: 'a new hello message',
                  metaData: [ contentType: 'application/json' ]
              ], [
                  contents: 'Hello',
                  description: 'a hello message',
                  metaData: [ contentType: 'application/json' ]
              ]
            ],
            metadata: BasePact.DEFAULT_METADATA
        ])

        when:
        pact.write(pactFile.parentFile.toString(), PactSpecVersion.V3)
        def json = new JsonSlurper().parse(pactFile)

        then:
        json.messages.size == 3
        json.messages*.description.toSet() == ['a hello message', 'a new hello message'].toSet()
        json.messages.find { it.description == 'a hello message' && !it.providerStates } == [contents: 'Hello',
            description: 'a hello message', metaData: [ contentType: 'application/json' ]]
    }

    def 'refuse to merge pacts with different spec versions'() {
        given:
        def json = new JsonSlurper().parse(pactFile)
        json.metadata['pact-specification'].version = '2.0.0'
        pactFile.write(new JsonBuilder(json).toPrettyString())

        def pact = new BasePact(new Provider(), new Consumer(), BasePact.DEFAULT_METADATA) {
            @Override
            Map toMap(PactSpecVersion pactSpecVersion) {
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

            @Override
            void mergeInteractions(List<Interaction> interactions) { }

            @SuppressWarnings('UnusedMethodParameter')
            @Override
            File fileForPact(String pactDir) { pactFile }

            List<Interaction> getInteractions() { [] }

            @Override
            Pact sortInteractions() { this }
        }

        when:
        pact.write('/some/pact/dir', PactSpecVersion.V3)

        then:
        InvalidPactException e = thrown()
        e.message.contains('Cannot merge pacts as they are not compatible')
    }

    def 'refuse to merge pacts with different types (message vs request-response)'() {
        given:
        def pactUrl = V3PactSpec.classLoader.getResource('v3-pact.json')
        pactFile.write(pactUrl.text)

        def pact = new BasePact(new Provider(), new Consumer(), BasePact.DEFAULT_METADATA) {
            @Override
            Map toMap(PactSpecVersion pactSpecVersion) {
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

            @Override
            void mergeInteractions(List<Interaction> interactions) { }

            @SuppressWarnings('UnusedMethodParameter')
            @Override
            File fileForPact(String pactDir) { pactFile }

            List<Interaction> getInteractions() { [] }

            @Override
            Pact sortInteractions() { this }
        }

        when:
        pact.write('/some/pact/dir', PactSpecVersion.V3)

        then:
        InvalidPactException e = thrown()
        e.message.contains('Cannot merge pacts as they are not compatible')
    }

}
