package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.PactVerificationResult
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
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
        def result = service.runTest { MockServer mockServer ->
          CloseableHttpClient httpclient = HttpClients.createDefault()
          httpclient.withCloseable {
            def data = MultipartEntityBuilder.create()
              .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
              .addBinaryBody('file', '1,2,3,4\n5,6,7,8'.bytes, ContentType.create('text/csv'), 'data.csv')
              .build()
            def request = RequestBuilder
              .post(mockServer.url + '/upload')
              .setEntity(data)
              .build()
            println('Executing request ' + request.requestLine)
            httpclient.execute(request)
          }
        }

        then:
        result == PactVerificationResult.Ok.INSTANCE
    }

}
