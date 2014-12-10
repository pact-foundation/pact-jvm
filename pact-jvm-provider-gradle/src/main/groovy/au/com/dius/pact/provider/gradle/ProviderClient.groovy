package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.Request
import groovyx.net.http.HTTPBuilder
import org.apache.http.HttpResponse
import scala.collection.JavaConverters$

class ProviderClient {

    Request request
    ProviderInfo provider

    HttpResponse makeRequest() {
        def client = newClient()
        def response
        def requestMap = [path: URLDecoder.decode(request.path(), 'UTF-8')]
        requestMap.headers = [:]
        if (request.headers().defined) {
            requestMap.headers += JavaConverters$.MODULE$.mapAsJavaMapConverter(request.headers().get()).asJava()
        }

        if (requestMap.headers['Content-Type']) {
            requestMap.requestContentType = requestMap.headers['Content-Type']
        } else {
            requestMap.requestContentType = 'application/json'
        }

        if (request.body().defined) {
            requestMap.body = request.body().get()
        }

        if (request.query().defined) {
            requestMap.query = request.query().get().split('&')*.split('=').inject([:]) { Map map, entry ->
                map[entry[0]] = (map[entry[0]] ?: []) << entry[1]
                map
            }
        }

        if (provider.requestFilter != null) {
            provider.requestFilter(requestMap)
        }

        client.handler.failure = { resp -> resp }
        client."${request.method().toLowerCase()}"(requestMap)
    }

  private HTTPBuilder newClient() {
    new HTTPBuilder("${provider.protocol}://${provider.host}:${provider.port}${provider.path}")
  }

}
