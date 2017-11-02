package au.com.dius.pact.provider.spring

import au.com.dius.pact.model.Request
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultHandler
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.web.util.UriComponentsBuilder

/**
 * Verifies the providers against the defined consumers using Spring MockMvc
 */
@Slf4j
@InheritConstructors
class MvcProviderVerifier extends ProviderVerifier {
    boolean debugRequestResponse = false

    void verifyResponseFromProvider(ProviderInfo provider, def interaction,
                                    String interactionMessage, Map failures, MockMvc mockMvc) {
        try {
            def request = interaction.request

            MvcResult mvcResult = mockMvc.perform(
                    request.body.isMissing() || request.body.isNull() || request.body.isEmpty() ?
                            MockMvcRequestBuilders.request(
                                    HttpMethod.valueOf(request.method),
                                    requestUriString(request)
                            )
                            .headers(mapHeaders(request, false))
                            :
                            MockMvcRequestBuilders.request(
                                    HttpMethod.valueOf(request.method),
                                    requestUriString(request)
                            )
                            .headers(mapHeaders(request, true))
                            .content(request.body.value)
            ).andDo(new ResultHandler() {
                @Override
                void handle(MvcResult result) throws Exception {
                    if (debugRequestResponse) {
                        MockMvcResultHandlers.print().handle(result)
                    }
                }
            }).andReturn()

            def expectedResponse = interaction.response
            def actualResponse = handleResponse(mvcResult.response)

            verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, failures)
        } catch (e) {
            failures[interactionMessage] = e
            reporters.each {
                it.requestFailed(provider, interaction, interactionMessage,
                        e, projectHasProperty.apply(PACT_SHOW_STACKTRACE))
            }
        }
    }

    def requestUriString(Request request) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath(request.path)

        Map<String, List<String>> query = request.query
        if (query != null && !query.isEmpty()) {
            query.each { key, value ->
                uriBuilder.queryParam(key, value.toArray(new String[value.size()]))
            }
        }

        URI.create(uriBuilder.toUriString())
    }

    def mapHeaders(Request request, boolean hasBody) {
        HttpHeaders httpHeaders = new HttpHeaders()

        request.headers.each { k, v ->
            httpHeaders.add(k, v)
        }

        if (hasBody && !httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
            httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        }

        httpHeaders
    }

    def handleResponse(MockHttpServletResponse httpResponse) {
        log.debug "Received response: ${httpResponse.status}"
        def response = [statusCode: httpResponse.status]

        response.headers = [:]
        httpResponse.headerNames.each { String headerName ->
            response.headers[headerName] = httpResponse.getHeader(headerName)
        }

        if (httpResponse.contentType) {
            response.contentType = org.apache.http.entity.ContentType.parse(httpResponse.contentType.toString())
        } else {
            response.contentType = org.apache.http.entity.ContentType.APPLICATION_JSON
        }
        response.data = httpResponse.contentAsString

        log.debug "Response: $response"

        response
    }
}
