package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.Request
import groovyx.net.http.RESTClient
import scala.collection.JavaConverters$

class ProviderClient {

    Request request
    ProviderInfo provider

    def makeRequest() {
        def client = new RESTClient(
                "${provider.protocol}://${provider.host}:${provider.port}${provider.path}")
        def response
        def requestMap = [path: request.path()]
        if (request.headers().defined) {
            requestMap.headers = [:] << JavaConverters$.MODULE$.asJavaMapConverter(request.headers().get()).asJava()
        }

        if (requestMap.headers['Content-Type']) {
            requestMap.requestContentType = requestMap.headers['Content-Type']
        } else {
            requestMap.requestContentType = 'application/json'
        }

        if (request.body().defined) {
            requestMap.body = request.bodyString().get()
        }

        client.handler.failure = { resp -> resp }
        switch (request.method()) {
            case 'POST':
                response = client.post(requestMap)
                break
            default:
                response = client.get(requestMap)
                break
        }

        println response.dump()

        response
    }

}
