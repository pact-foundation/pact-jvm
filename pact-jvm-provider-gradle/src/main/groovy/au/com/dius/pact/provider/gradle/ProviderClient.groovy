package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.Request
import groovyx.net.http.RESTClient
import org.apache.http.HttpResponse
import scala.collection.JavaConverters$

class ProviderClient {

    Request request
    ProviderInfo provider

    HttpResponse makeRequest() {
        def client = new RESTClient(
                "${provider.protocol}://${provider.host}:${provider.port}${provider.path}")
        def response
        def requestMap = [path: request.path()]
        requestMap.headers = [:]
        if (request.headers().defined) {
            requestMap.headers = JavaConverters$.MODULE$.asJavaMapConverter(request.headers().get()).asJava()
        }

        if (requestMap.headers['Content-Type']) {
            requestMap.requestContentType = requestMap.headers['Content-Type']
        } else {
            requestMap.requestContentType = 'application/json'
        }

        if (request.body().defined) {
            requestMap.body = request.body().get()
        }

        client.handler.failure = { resp -> resp }
        switch (request.method()) {
            case 'POST':
                response = client.post(requestMap)
                break
            case 'HEAD':
                response = client.head(requestMap)
                break
            case 'OPTIONS':
                response = client.options(requestMap)
                break
            case 'PUT':
                response = client.put(requestMap)
                break
            case 'DELETE':
                response = client.delete(requestMap)
                break
            case 'PATCH':
                response = client.patch(requestMap)
                break
            default:
                response = client.get(requestMap)
                break
        }

        response
    }

}
