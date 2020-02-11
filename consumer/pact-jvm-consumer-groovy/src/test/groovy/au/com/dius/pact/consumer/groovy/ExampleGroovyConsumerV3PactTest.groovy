package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactConsumerConfig
import au.com.dius.pact.consumer.PactVerificationResult
import groovy.json.JsonSlurper
import groovyx.net.http.ContentTypes
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import org.junit.Test

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExampleGroovyConsumerV3PactTest {

    @Test
    void "example V3 spec test"() {

        LocalDate localDate = LocalDate.now()
        def aliceService = new PactBuilder()
        aliceService {
            serviceConsumer 'V3Consumer'
            hasPactWith 'V3Service'
        }

        aliceService {
            given('a provider state')
            given('another provider state', [valueA: 'A', valueB: 100])
            given('a third provider state', [valueC: localDate.toString()])
            uponReceiving('a retrieve Mallory request')
            withAttributes(method: 'get', path: '/mallory', query: [name: 'ron', status: regexp(~/good|bad/, 'good'),
              date: date('yyyy-MM-dd')])
            willRespondWith(
                status: 200,
                headers: ['Content-Type': 'text/html'],
                body: '"That is some good Mallory."'
            )
        }

        PactVerificationResult result = aliceService.runTest { mockServer ->
            def client = HttpBuilder.configure {
                request.uri = mockServer.url
            }
            def aliceResponse = client.get(FromServer){
                request.uri.path = '/mallory'
                request.uri.query = [status: 'good', name: 'ron',
                                     date: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)]
                response.parser(ContentTypes.ANY) { config, resp ->
                    return resp
                }
            }

            assert aliceResponse.statusCode == 200
            assert aliceResponse.contentType == 'text/html'

            def data = aliceResponse.inputStream.text
            assert data == '"That is some good Mallory."'
        }
        assert result instanceof PactVerificationResult.Ok

      def pactFile = new File("${PactConsumerConfig.INSTANCE.pactDirectory}/V3Consumer-V3Service.json")
      def json = new JsonSlurper().parse(pactFile)
      assert json.metadata.pactSpecification.version == '3.0.0'
      def providerStates = json.interactions.first().providerStates
      assert providerStates.size() == 3
      assert providerStates[0] == [name: 'a provider state']
      assert providerStates[1] == [name: 'another provider state', params: [valueA: 'A', valueB: 100]]
      assert providerStates[2] == [name: 'a third provider state',
                                   params: [valueC: localDate.toString()]
      ]
    }
}
