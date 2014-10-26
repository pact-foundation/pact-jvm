package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactMismatch
import au.com.dius.pact.consumer.PactVerified$
import au.com.dius.pact.consumer.VerificationResult
import groovy.json.JsonBuilder
import groovyx.net.http.RESTClient
import org.junit.Test

class ExampleGroovyConsumerPactTest {

    @Test
    void "A service consumer side of a pact goes a little something like this"() {

        def alice_service = new PactBuilder()
        alice_service {
            serviceConsumer "Consumer"
            hasPactWith "Alice Service"
            port 1234
        }

        def bob_service = new PactBuilder().build {
            serviceConsumer "Consumer"
            hasPactWith "Bob"
        }

        alice_service {
            uponReceiving('a retrieve Mallory request')
            withAttributes(method: 'get', path: '/mallory', query: [name: 'ron', status: 'good'])
            willRespondWith(
                status: 200,
                headers: ['Content-Type': 'text/html'],
                body: '"That is some good Mallory."'
            )
        }

        bob_service {
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
            withAttributes(method: 'put', path: '/alligators', body: [ ['name' : 'Roger' ] ])
            willRespondWith(status: 200, body: [ ["name": "Roger", "age": 20 ] ], headers: ['Content-Type': 'application/json'])
        }

        VerificationResult result = alice_service.run() {
            def client = new RESTClient('http://localhost:1234/')
            def alice_response = client.get(path: '/mallory', query: [status: 'good', name: 'ron'])

            assert alice_response.status == 200
            assert alice_response.contentType == 'text/html'

            def data = alice_response.data.text()
            assert data == '"That is some good Mallory."'
        }
        assert result == PactVerified$.MODULE$

        result = bob_service.run() { config ->
            def client = new RESTClient(config.url())
            def body = new JsonBuilder([name: 'Bobby'])
            def bob_post_response = client.post(path: '/donuts', requestContentType: 'application/json',
                headers: [
                    'Accept': 'text/plain',
                    'Content-Type': 'application/json'
                ], body: body.toPrettyString()
            )

            assert bob_post_response.status == 201
            assert bob_post_response.data.text == '"Donut created."'

            body = new JsonBuilder([ [name: 'Roger'] ])
            def bob_put_response = client.put(path: '/alligators', requestContentType: 'application/json',
                headers: [ 'Content-Type': 'application/json' ], body: body.toPrettyString()
            )

            assert bob_put_response.status == 200
            assert bob_put_response.data == [ [age:20, name:'Roger'] ]
        }
        assert result instanceof PactMismatch
        assert result.results.missing.size() == 1
    }
}
