package au.com.dius.pact.model

import au.com.dius.pact.model.v3.messaging.MessagePact
import spock.lang.Specification

@SuppressWarnings('DuplicateMapLiteral')
class PactReaderSpec extends Specification {

    def 'loads a pact with no metadata as V1'() {
        given:
        def pactUrl = PactReaderSpec.classLoader.getResource('pact.json')
        def pactLoader = Spy(PactReader)

        when:
        def pact = pactLoader.loadPact(pactUrl)

        then:
        1 * pactLoader.loadV2Pact(pactUrl, _)
        0 * pactLoader.loadV3Pact(pactUrl, _)
        pact instanceof Pact
    }

    def 'loads a pact with V1 version using existing loader'() {
        given:
        def pactUrl = PactReaderSpec.classLoader.getResource('v1-pact.json')
        def pactLoader = Spy(PactReader)

        when:
        def pact = pactLoader.loadPact(pactUrl)

        then:
        1 * pactLoader.loadV2Pact(pactUrl, _)
        0 * pactLoader.loadV3Pact(pactUrl, _)
        pact instanceof Pact
    }

    def 'loads a pact with V2 version using existing loader'() {
        given:
        def pactUrl = PactReaderSpec.classLoader.getResource('v2-pact.json')
        def pactLoader = Spy(PactReader)

        when:
        def pact = pactLoader.loadPact(pactUrl)

        then:
        1 * pactLoader.loadV2Pact(pactUrl, _)
        0 * pactLoader.loadV3Pact(pactUrl, _)
        pact instanceof Pact
    }

    def 'loads a pact with V3 version using V2 loader'() {
        given:
        def pactUrl = PactReaderSpec.classLoader.getResource('v3-pact.json')
        def pactLoader = Spy(PactReader)

        when:
        def pact = pactLoader.loadPact(pactUrl)

        then:
        1 * pactLoader.loadV2Pact(pactUrl, _)
        1 * pactLoader.loadV3Pact(pactUrl, _)
        pact instanceof Pact
    }

    def 'loads a message pact with V3 version using V3 loader'() {
        given:
        def pactUrl = PactReaderSpec.classLoader.getResource('v3-message-pact.json')
        def pactLoader = Spy(PactReader)

        when:
        def pact = pactLoader.loadPact(pactUrl)

        then:
        0 * pactLoader.loadV2Pact(pactUrl, _)
        1 * pactLoader.loadV3Pact(pactUrl, _)
        pact instanceof MessagePact
    }

    def 'loads a pact from an inputstream'() {
        given:
        def pactInputStream = PactReaderSpec.classLoader.getResourceAsStream('pact.json')
        def pactLoader = Spy(PactReader)

        when:
        def pact = pactLoader.loadPact(pactInputStream)

        then:
        1 * pactLoader.loadV2Pact(pactInputStream, _)
        0 * pactLoader.loadV3Pact(pactInputStream, _)
        pact instanceof Pact
    }

}
