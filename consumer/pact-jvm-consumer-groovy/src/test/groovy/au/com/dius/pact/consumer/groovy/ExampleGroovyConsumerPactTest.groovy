package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.ContentTypes
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
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
            def client = HttpBuilder.configure {
                request.uri = 'http://localhost:1233/'
            }
            def aliceResponse = client.get(FromServer) {
                request.uri.path = '/mallory'
                request.uri.query = [status: 'good', name: 'ron']
                response.parser(ContentTypes.HTML) { config, resp ->
                    return resp
                }
            }

            assert aliceResponse.statusCode == 200
            assert aliceResponse.contentType == 'text/html'

            def data = aliceResponse.inputStream.text
            assert data == '"That is some good Mallory."'
        }
        assert result instanceof PactVerificationResult.Ok

        result = bobService.runTest { mockServer, context ->
            def client = HttpBuilder.configure {
                request.uri = mockServer.url
            }
            def body = new JsonBuilder([name: 'Bobby'])
            def bobPostResponse = client.post(FromServer) {
                request.uri.path = '/donuts'
                request.contentType = 'application/json'
                request.headers = [
                        'Accept'      : 'text/plain',
                        'Content-Type': 'application/json'
                ]
                request.body = body.toPrettyString()
                response.parser(ContentTypes.TEXT) { config, resp ->
                    return resp
                }
            }

            assert bobPostResponse.statusCode == 201
            assert bobPostResponse.inputStream.text == '"Donut created."'

            body = new JsonBuilder([ [name: 'Roger'] ])
            def bobPutResponse = client.put(FromServer){
                request.uri.path = "/alligators"
                request.contentType = 'application/json'
                request.headers = [ 'Content-Type': 'application/json' ]
                request.body = body.toPrettyString()
                response.parser(ContentTypes.ANY) { config, resp ->
                    return resp
                }
            }

            assert bobPutResponse.statusCode == 200
            assert new JsonSlurper().parse(bobPutResponse.inputStream) == [[age: 20, name: 'Roger'] ]
        }
        assert result instanceof PactVerificationResult.ExpectedButNotReceived
        assert result.expectedRequests.size() == 1
    }
}
