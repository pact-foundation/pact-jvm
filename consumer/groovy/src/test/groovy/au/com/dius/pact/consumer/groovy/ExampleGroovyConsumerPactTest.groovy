package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.core.support.SimpleHttp
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.junit.Test

class ExampleGroovyConsumerPactTest {

    @Test
    @SuppressWarnings('AbcMetric')
    void "A service consumer side of a pact goes a little something like this"() {

        def aliceService = new PactBuilder()
        aliceService {
            serviceConsumer 'Consumer'
            hasPactWith 'Alice Service'
            port 1233
        }

        def bobService = new PactBuilder().build {
            serviceConsumer 'Consumer'
            hasPactWith 'Bob'
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

        bobService {
            uponReceiving('a create donut request')
            withAttributes(method: 'post', path: '/donuts',
                headers: ['Accept': 'text/plain', 'Content-Type': 'application/json']
            )
            withBody {
              name regexp(~/Bob.*/, 'Bob')
            }
            willRespondWith(status: 201, body: '"Donut created."', headers: ['Content-Type': 'text/plain'])

            uponReceiving('a delete charlie request')
            withAttributes(method: 'delete', path: '/charlie')
            willRespondWith(status: 200, body: '"deleted"', headers: ['Content-Type': 'text/plain'])

            uponReceiving('an update alligators request')
            withAttributes(method: 'put', path: '/alligators', body: [ ['name': 'Roger' ] ],
              headers: ['Content-Type': 'application/json'])
            willRespondWith(status: 200, body: [ [name: 'Roger', age: 20] ],
                headers: ['Content-Type': 'application/json'])
        }

        PactVerificationResult result = aliceService.runTest {
            def client = new SimpleHttp('http://localhost:1233/')
            def aliceResponse = client.get('/mallory', [status: 'good', name: 'ron'])

            assert aliceResponse.statusCode == 200
            assert aliceResponse.contentType == 'text/html'

            def data = aliceResponse.inputStream.text
            assert data == '"That is some good Mallory."'
        }
        assert result instanceof PactVerificationResult.Ok

        result = bobService.runTest { mockServer, context ->
            def client = new SimpleHttp(mockServer.url)
            def body = new JsonBuilder([name: 'Bobby'])
            def bobPostResponse = client.post('/donuts', body.toPrettyString(),
              'application/json', [ 'Accept': 'text/plain' ])

            assert bobPostResponse.statusCode == 201
            assert bobPostResponse.inputStream.text == '"Donut created."'

            body = new JsonBuilder([ [name: 'Roger'] ])
            def bobPutResponse = client.put('/alligators', body.toPrettyString(), 'application/json')
                //request.headers = [ 'Content-Type': 'application/json' ]

            assert bobPutResponse.statusCode == 200
            assert new JsonSlurper().parse(bobPutResponse.inputStream) == [[age: 20, name: 'Roger'] ]
        }
        assert result instanceof PactVerificationResult.ExpectedButNotReceived
        assert result.expectedRequests.size() == 1
    }
}
