package au.com.dius.pact.provider

import au.com.dius.pact.model.Request
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import org.apache.http.Consts
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpHead
import org.apache.http.client.methods.HttpOptions
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpTrace
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import scala.Function1

/**
 * Client HTTP utility for providers
 */
@Slf4j
class ProviderClient {

    private static final String CONTENT_TYPE = 'Content-Type'
    private static final String UTF8 = 'UTF-8'
    public static final String REQUEST = 'request'

    HttpClientFactory httpClientFactory = new HttpClientFactory()
    Request request
    def provider

    def makeRequest() {
        log.debug "Making request for provider $provider:"
        log.debug request.toString()

        CloseableHttpClient httpclient = httpClientFactory.newClient(provider)
        HttpRequest method = newRequest(request)
        setupHeaders(method)
        setupBody(method)

        executeRequestFilter(method)

        def response = httpclient.execute(method)
        try {
            return handleResponse(response)
        } finally {
            response.close()
        }
    }

    private void executeRequestFilter(HttpRequest method) {
        if (provider.requestFilter != null) {
            if (provider.requestFilter instanceof Closure) {
                provider.requestFilter(method)
            } else if (provider.requestFilter instanceof Function1) {
              provider.requestFilter.apply(method)
            } else if (provider.requestFilter instanceof org.apache.commons.collections.Closure) {
              provider.requestFilter.execute(method)
            } else {
                Binding binding = new Binding()
                binding.setVariable(REQUEST, method)
                GroovyShell shell = new GroovyShell(binding)
                shell.evaluate(provider.requestFilter as String)
            }
        }
    }

    private void setupBody(HttpRequest method) {
        if (method instanceof HttpEntityEnclosingRequest) {
            if (urlEncodedFormPost(request) && request.query != null) {
                def charset = Consts.UTF_8
                List parameters = request.query.collectMany { entry ->
                    entry.value.collect {
                        new BasicNameValuePair(entry.key, it)
                    }
                }
                method.setEntity(new UrlEncodedFormEntity(parameters, charset))
            } else if (request.body != null) {
                method.setEntity(new StringEntity(request.body))
            }
        }
    }

    private void setupHeaders(HttpRequest method) {
        if (request.headers != null) {
            request.headers.each { key, value ->
                method.addHeader(key, value)
            }

            if (!method.containsHeader(CONTENT_TYPE)) {
                method.addHeader(CONTENT_TYPE, 'application/json')
            }
        }
    }

    CloseableHttpResponse makeStateChangeRequest(def stateChangeUrl, String state, boolean postStateInBody) {
        if (stateChangeUrl) {
            CloseableHttpClient httpclient = httpClientFactory.newClient(provider)
            def urlBuilder
            if (stateChangeUrl instanceof URI) {
                urlBuilder = new URIBuilder(stateChangeUrl)
            } else {
                urlBuilder = new URIBuilder(stateChangeUrl.toString())
            }
            HttpRequest method

            if (postStateInBody) {
                method = new HttpPost(urlBuilder.build())
                method.setEntity(new StringEntity(new JsonBuilder([state: state]).toPrettyString(),
                        ContentType.APPLICATION_JSON))
            } else {
                method = new HttpPost(urlBuilder.setParameter('state', state).build())
            }

            if (provider.stateChangeRequestFilter != null) {
                if (provider.stateChangeRequestFilter instanceof Closure) {
                  provider.stateChangeRequestFilter(method)
                } else if (provider.stateChangeRequestFilter instanceof Function1) {
                  provider.stateChangeRequestFilter.apply(method)
                } else {
                    Binding binding = new Binding()
                    binding.setVariable(REQUEST, method)
                    GroovyShell shell = new GroovyShell(binding)
                    shell.evaluate(provider.stateChangeRequestFilter as String)
                }
            }

            httpclient.execute(method)
        }
    }

    def handleResponse(HttpResponse httpResponse) {
        log.debug "Received response: ${httpResponse.statusLine}"
        def response = [statusCode: httpResponse.statusLine.statusCode]

        response.headers = [:]
        httpResponse.allHeaders.each { Header header ->
            response.headers[header.name] = header.value
        }

        HttpEntity entity = httpResponse.entity
        if (entity != null) {
            if (entity.contentType) {
                response.contentType = ContentType.parse(entity.contentType.value)
            } else {
                response.contentType = ContentType.APPLICATION_JSON
            }
            response.data = EntityUtils.toString(entity, response.contentType?.charset?.name() ?: UTF8)
        }

        log.debug "Response: $response"

        response
    }

    private HttpRequest newRequest(Request request) {
        def urlBuilder = new URIBuilder()
        urlBuilder.scheme = provider.protocol
        if (provider.host instanceof Closure) {
            urlBuilder.host = provider.host.call()
        } else {
            urlBuilder.host = provider.host
        }
        urlBuilder.port = provider.port

        String path = ''
        if (provider.path.size() > 0) {
            path = provider.path.toString()
            if (path[-1] == '/') {
                path = path.size() > 1 ? path[0..-2] : ''
            }
        }

        path += URLDecoder.decode(request.path, UTF8)
        urlBuilder.path = path

        if (request.query != null && !urlEncodedFormPost(request)) {
          request.query.each {
            entry -> entry.value.each {
              urlBuilder.addParameter(entry.key, it)
            }
          }
        }

        def url = urlBuilder.build().toString()
        switch (request.method.toLowerCase()) {
            case 'post':
                return new HttpPost(url)
            case 'put':
                return new HttpPut(url)
            case 'options':
                return new HttpOptions(url)
            case 'delete':
                return new HttpDelete(url)
            case 'head':
                return new HttpHead(url)
            case 'patch':
                return new HttpPatch(url)
            case 'trace':
                return new HttpTrace(url)
            default:
                return new HttpGet(url)
        }
    }

    static boolean urlEncodedFormPost(Request request) {
        request.method.toLowerCase() == 'post' &&
                request.mimeType() == ContentType.APPLICATION_FORM_URLENCODED.mimeType
    }
}
