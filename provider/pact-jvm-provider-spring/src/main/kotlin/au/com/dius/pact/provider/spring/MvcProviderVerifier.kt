package au.com.dius.pact.provider.spring

import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import mu.KLogging
import org.apache.commons.lang3.StringUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import javax.mail.internet.ContentDisposition
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource
import org.hamcrest.Matchers.anything

/**
 * Verifies the providers against the defined consumers using Spring MockMvc
 */
open class MvcProviderVerifier(private val debugRequestResponse: Boolean = false) : ProviderVerifier() {

  fun verifyResponseFromProvider(
    provider: ProviderInfo,
    interaction: RequestResponseInteraction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    mockMvc: MockMvc
  ) {
    try {
      val request = interaction.request

      val mvcResult = executeMockMvcRequest(mockMvc, request)

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

  fun executeMockMvcRequest(mockMvc: MockMvc, request: Request): MvcResult {
    val body = request.body
    val requestBuilder = if (body != null && body.isPresent()) {
      if (request.isMultipartFileUpload()) {
        val multipart = MimeMultipart(ByteArrayDataSource(body.unwrap(), request.contentTypeHeader()))
        val bodyPart = multipart.getBodyPart(0)
        val contentDisposition = ContentDisposition(bodyPart.getHeader("Content-Disposition").first())
        val name = StringUtils.defaultString(contentDisposition.getParameter("name"), "file")
        val filename = contentDisposition.getParameter("filename").orEmpty()
        MockMvcRequestBuilders.fileUpload(requestUriString(request))
          .file(MockMultipartFile(name, filename, bodyPart.contentType, bodyPart.inputStream))
          .headers(mapHeaders(request, true))
      } else {
        MockMvcRequestBuilders.request(HttpMethod.valueOf(request.method), requestUriString(request))
          .headers(mapHeaders(request, true))
          .content(body.value)
      }
    } else {
      MockMvcRequestBuilders.request(HttpMethod.valueOf(request.method), requestUriString(request))
        .headers(mapHeaders(request, false))
    }
      return performRequest(mockMvc, requestBuilder).andDo({
      if (debugRequestResponse) {
        MockMvcResultHandlers.print().handle(it)
      }
    }).andReturn()
  }

  private fun performRequest(mockMvc: MockMvc, requestBuilder: RequestBuilder): ResultActions {
    val resultActions = mockMvc.perform(requestBuilder)
    return if (resultActions.andReturn().request.isAsyncStarted) {
      mockMvc.perform(asyncDispatch(resultActions
        .andExpect(request().asyncResult(anything()))
        .andReturn()))
    } else {
      resultActions
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

    request.headers?.forEach { k, v ->
      httpHeaders.add(k, v.joinToString(", "))
    }

    if (hasBody && !httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
      httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    }

    return httpHeaders
  }

  fun handleResponse(httpResponse: MockHttpServletResponse): Map<String, Any> {
    logger.debug { "Received response: ${httpResponse.status}" }
    val response = mutableMapOf<String, Any>("statusCode" to httpResponse.status)

    val headers = mutableMapOf<String, List<String>>()
    httpResponse.headerNames.forEach { headerName ->
      headers[headerName] = listOf(httpResponse.getHeader(headerName))
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
