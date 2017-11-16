package au.com.dius.pact.provider.spring

import au.com.dius.pact.model.Request
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import groovy.lang.MetaClass
import mu.KLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * Verifies the providers against the defined consumers using Spring MockMvc
 */
open class MvcProviderVerifier(private val debugRequestResponse: Boolean = false): ProviderVerifier() {

  fun verifyResponseFromProvider(provider: ProviderInfo, interaction: RequestResponseInteraction,
                                 interactionMessage: String, failures: MutableMap<String, Any>, mockMvc: MockMvc) {
    try {
      val request = interaction.request

      val mvcResult = mockMvc.perform(
        if (request.body.isPresent()) {
          MockMvcRequestBuilders.request(HttpMethod.valueOf(request.method), requestUriString(request))
            .headers(mapHeaders(request, true))
            .content(request.body.value)
        } else {
          MockMvcRequestBuilders.request(HttpMethod.valueOf(request.method), requestUriString(request))
            .headers(mapHeaders(request, false))
        }
      ).andDo({
        if (debugRequestResponse) {
          MockMvcResultHandlers.print().handle(it)
        }
      }).andReturn()

      val expectedResponse = interaction.response
      val actualResponse = handleResponse(mvcResult.response)

      verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, failures)
    } catch (e: Exception) {
      failures[interactionMessage] = e
      reporters.forEach {
        it.requestFailed(provider, interaction, interactionMessage, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE))
      }
    }
  }

  fun requestUriString(request: Request): URI {
    val uriBuilder = UriComponentsBuilder.fromPath(request.path)

    val query = request.query
    if (query != null && query.isNotEmpty()) {
      query.forEach { key, value ->
        uriBuilder.queryParam(key, *value.toTypedArray())
      }
    }

    return URI.create(uriBuilder.toUriString())
  }

  fun mapHeaders(request: Request, hasBody: Boolean): HttpHeaders {
    val httpHeaders = HttpHeaders()

    request.headers.forEach { k, v ->
      httpHeaders.add(k, v)
    }

    if (hasBody && !httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
      httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    }

    return httpHeaders
  }

  fun handleResponse(httpResponse: MockHttpServletResponse): Map<String, Any> {
    logger.debug { "Received response: ${httpResponse.status}" }
    val response = mutableMapOf<String, Any>("statusCode" to httpResponse.status)

    val headers = mutableMapOf<String, String>()
    httpResponse.headerNames.forEach { headerName ->
      headers[headerName] = httpResponse.getHeader(headerName)
    }
    response["headers"] = headers

    if (httpResponse.contentType.isNullOrEmpty()) {
      response["contentType"] = org.apache.http.entity.ContentType.APPLICATION_JSON
    } else {
      response["contentType"] = org.apache.http.entity.ContentType.parse(httpResponse.contentType.toString())
    }
    response["data"] = httpResponse.contentAsString

    logger.debug { "Response: $response" }

    return response
  }

  companion object : KLogging()
}
