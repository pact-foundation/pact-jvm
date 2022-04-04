package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.core.support.SimpleHttp
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
            def http = new SimpleHttp('http://localhost:8000')
            def response = http.post('/path', 'number=12345678', 'application/x-www-form-urlencoded')
            assert response.statusCode == 201
        } instanceof PactVerificationResult.Ok
    }
}
