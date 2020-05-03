package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import groovyx.net.http.ContentTypes
import groovyx.net.http.HttpBuilder
import org.junit.Test

class ExampleFormPostTest {

    @Test
    void 'handles bodies from form posts'() {
        def service = new PactBuilder()
        service {
            serviceConsumer 'Consumer'
            hasPactWith 'Old School Service'
            port 8000

            uponReceiving('a POST in application/x-www-form-urlencoded')
            withAttributes(method: 'post', path: '/path',
                headers: ['Content-Type': 'application/x-www-form-urlencoded'],
                body: 'number=12345678'
            )
            willRespondWith(status: 201, body: 'form posted ok', headers: ['Content-Type': 'text/plain'])
        }

        assert service.runTest {
            def http = HttpBuilder.configure { request.uri = 'http://localhost:8000' }
            http.post {
                request.uri.path = '/path'
                request.body = [number: '12345678']
                request.contentType = ContentTypes.URLENC[0]
                response.parser(ContentTypes.ANY[0]) { config, resp ->
                    assert resp.statusCode == 201
                }
            }
        } instanceof PactVerificationResult.Ok
    }

}
