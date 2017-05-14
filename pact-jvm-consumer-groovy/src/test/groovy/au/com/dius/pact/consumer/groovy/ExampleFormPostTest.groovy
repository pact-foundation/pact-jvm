package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
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

        assert PactVerificationResult.Ok.INSTANCE == service.runTest {
            def http = new HTTPBuilder( 'http://localhost:8000' )
            http.post(path: '/path', body: [number: '12345678'], requestContentType: ContentType.URLENC) { resp ->
                assert resp.statusLine.statusCode == 201
            }
        }
    }

}
