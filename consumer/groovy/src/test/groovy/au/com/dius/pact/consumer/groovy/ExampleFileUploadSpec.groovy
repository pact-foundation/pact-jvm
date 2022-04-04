package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.PactVerificationResult
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ContentType
import spock.lang.Specification

class ExampleFileUploadSpec extends Specification {

    def 'handles bodies from form posts'() {
        given:
        def service = new PactBuilder()
        service {
            serviceConsumer 'Consumer'
            hasPactWith 'File Service'
            uponReceiving('a multipart file POST')
            withAttributes(path: '/upload', method: 'post')
            withFileUpload('file', 'data.csv', 'text/csv', '1,2,3,4\n5,6,7,8'.bytes)
            willRespondWith(status: 201, body: 'file uploaded ok', headers: ['Content-Type': 'text/plain'])
        }

        when:
        def result = service.runTest { MockServer mockServer, context ->
          CloseableHttpClient httpclient = HttpClients.createDefault()
          httpclient.withCloseable {
            def data = MultipartEntityBuilder.create()
              .setMode(HttpMultipartMode.EXTENDED)
              .addBinaryBody('file', '1,2,3,4\n5,6,7,8'.bytes, ContentType.create('text/csv'), 'data.csv')
              .build()
            def request = new HttpPost(mockServer.url + '/upload')
            request.setEntity(data)
            httpclient.execute(request)
          }
        }

        then:
        result instanceof PactVerificationResult.Ok
    }
}
