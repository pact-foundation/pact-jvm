package au.com.dius.pact.provider.spring

import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import groovy.transform.InheritConstructors
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultHandler
import org.springframework.web.util.UriComponentsBuilder

/**
 * Verifies the providers against the defined consumers using Spring MockMvc
 */
@InheritConstructors
class MvcProviderVerifier extends ProviderVerifier {
    boolean debugRequestResponse = false

    void verifyResponseFromProvider(ProviderInfo provider, def interaction,
                                    String interactionMessage, Map failures, MockMvc mockMvc) {
        try {
            def request = interaction.request()
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath(request.path())

            Map<String, List<String>> query = request.query()
            if (query != null && !query.isEmpty()) {
                query.each { key, value ->
                    uriBuilder.queryParam(key, value.toArray(new String[value.size()]))
                }
            }

            MvcResult mvcResult = mockMvc.perform(
                    request.body.isMissing() || request.body.isNull() || request.body.isEmpty() ?
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                    org.springframework.http.HttpMethod.valueOf(request.method()),
                                    URI.create(uriBuilder.toUriString())
                            )
                            :
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                    org.springframework.http.HttpMethod.valueOf(request.method()),
                                    URI.create(uriBuilder.toUriString())
                            ).contentType(MediaType.APPLICATION_JSON).content(request.body.value)
            ).andDo(new ResultHandler() {
                @Override
                void handle(MvcResult result) throws Exception {
                    if (debugRequestResponse) {
                        org.springframework.test.web.servlet.result.MockMvcResultHandlers.print().handle(result)
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
                        e, callProjectHasProperty(PACT_SHOW_STACKTRACE))
            }
        }
    }

    def handleResponse(MockHttpServletResponse httpResponse) {
        log.debug "Received response: ${httpResponse.status}"
        def response = [statusCode: httpResponse.status]

        response.headers = [:]
        httpResponse.headerNames().each { String headerName ->
            response.headers[headerName] = httpResponse.header(headerName)
        }

        if (httpResponse.contentType) {
            response.contentType = org.apache.http.entity.ContentType.parse(httpResponse.contentType.toString())
        } else {
            response.contentType = org.apache.http.entity.ContentType.APPLICATION_JSON
        }
        response.data = httpResponse.contentAsString()

        log.debug "Response: $response"

        response
    }
}
