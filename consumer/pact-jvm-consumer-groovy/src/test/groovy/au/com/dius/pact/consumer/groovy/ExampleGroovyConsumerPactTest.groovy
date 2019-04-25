package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import groovy.json.JsonBuilder
import groovyx.net.http.RESTClient
import org.junit.Test

class ExampleGroovyConsumerPactTest {

    @Test
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
            def client = new RESTClient('http://localhost:1233/')
            def aliceResponse = client.get(path: '/mallory', query: [status: 'good', name: 'ron'])

            assert aliceResponse.status == 200
            assert aliceResponse.contentType == 'text/html'

            def data = aliceResponse.data.text()
            assert data == '"That is some good Mallory."'
        }
        assert result instanceof PactVerificationResult.Ok

        result = bobService.runTest { mockServer, context ->
            def client = new RESTClient(mockServer.url)
            def body = new JsonBuilder([name: 'Bobby'])
            def bobPostResponse = client.post(path: '/donuts', requestContentType: 'application/json',
                headers: [
                    'Accept': 'text/plain',
                    'Content-Type': 'application/json'
                ], body: body.toPrettyString()
            )

            assert bobPostResponse.status == 201
            assert bobPostResponse.data.text == '"Donut created."'

            body = new JsonBuilder([ [name: 'Roger'] ])
            def bobPutResponse = client.put(path: '/alligators', requestContentType: 'application/json',
                headers: [ 'Content-Type': 'application/json' ], body: body.toPrettyString()
            )

            assert bobPutResponse.status == 200
            assert bobPutResponse.data == [ [age: 20, name: 'Roger'] ]
        }
        assert result instanceof PactVerificationResult.ExpectedButNotReceived
        assert result.expectedRequests.size() == 1
    }
}
