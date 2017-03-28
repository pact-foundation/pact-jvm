package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactConsumerConfig
@SuppressWarnings('UnusedImport')
import au.com.dius.pact.consumer.PactVerified$
import au.com.dius.pact.consumer.VerificationResult
import au.com.dius.pact.model.PactSpecVersion
import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient
import org.junit.Test

class ExampleGroovyConsumerV3PactTest {

    @Test
    void "example V3 spec test"() {

        def aliceService = new PactBuilder()
        aliceService {
            serviceConsumer 'V3Consumer'
            hasPactWith 'V3Service'
            port 1254
        }

        aliceService {
            uponReceiving('a retrieve Mallory request')
            withAttributes(method: 'get', path: '/mallory', query: [name: 'ron', status: 'good'])
            willRespondWith(
                status: 200,
                headers: ['Content-Type': 'text/html'],
                body: '"That is some good Mallory."'
            )
        }

        VerificationResult result = aliceService.run(specificationVersion: PactSpecVersion.V3) {
            def client = new RESTClient('http://localhost:1254/')
            def aliceResponse = client.get(path: '/mallory', query: [status: 'good', name: 'ron'])

            assert aliceResponse.status == 200
            assert aliceResponse.contentType == 'text/html'

            def data = aliceResponse.data.text()
            assert data == '"That is some good Mallory."'
        }
        assert result == PactVerified$.MODULE$

      def pactFile = new File("${PactConsumerConfig.pactRootDir()}/V3Consumer-V3Service.json")
      def json = new JsonSlurper().parse(pactFile)
      assert json.metadata['pact-specification'].version == '3.0.0'

    }
}
