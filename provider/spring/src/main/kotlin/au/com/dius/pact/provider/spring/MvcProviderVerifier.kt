package au.com.dius.pact.provider.spring

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.IRequest
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.SynchronousRequestResponse
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderResponse
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.VerificationFailureType
import au.com.dius.pact.provider.VerificationResult
import groovy.lang.Binding
import groovy.lang.Closure
import groovy.lang.GroovyShell
import mu.KLogging
import org.apache.commons.lang3.StringUtils
import org.hamcrest.Matchers.anything
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.concurrent.Callable
import java.util.function.Consumer
import java.util.function.Function
import javax.mail.internet.ContentDisposition
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

/**
 * Verifies the providers against the defined consumers using Spring MockMvc
 */
open class MvcProviderVerifier(private val debugRequestResponse: Boolean = false) : ProviderVerifier() {

  fun verifyResponseFromProvider(
    provider: ProviderInfo,
    interaction: SynchronousRequestResponse,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    mockMvc: MockMvc,
    pending: Boolean
  ): VerificationResult {
    return try {
      val request = interaction.request

      val mvcResult = executeMockMvcRequest(mockMvc, request, provider)

      val expectedResponse = interaction.response
      val actualResponse = handleResponse(mvcResult.response)

      verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, failures,
        interaction.interactionId.orEmpty(), false)
    } catch (e: Exception) {
      failures[interactionMessage] = e
      reporters.forEach {
        it.requestFailed(provider, interaction, interactionMessage, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE))
      }
      return VerificationResult.Failed("Request to provider method failed with an exception", interactionMessage,
        mapOf(interaction.interactionId.orEmpty() to listOf(
          VerificationFailureType.ExceptionFailure("Request to provider method failed with an exception", e))),
        pending)
    }
  }

  fun executeMockMvcRequest(mockMvc: MockMvc, request: IRequest, provider: ProviderInfo): MvcResult {
    val body = request.body
    val requestBuilder = if (body.isPresent()) {
      if (request.isMultipartFileUpload()) {
        val multipart = MimeMultipart(ByteArrayDataSource(body.unwrap(), request.contentTypeHeader()))
        val multipartRequest = MockMvcRequestBuilders.multipart(requestUriString(request))
        var i = 0
        while (i < multipart.count) {
          val bodyPart = multipart.getBodyPart(i)
          val contentDisposition = ContentDisposition(bodyPart.getHeader("Content-Disposition").first())
          val name = StringUtils.defaultString(contentDisposition.getParameter("name"), "file")
          val filename = contentDisposition.getParameter("filename")
          if (filename.isNullOrEmpty()) {
            multipartRequest.param(name, bodyPart.content.toString())
          } else {
            multipartRequest.file(MockMultipartFile(name, filename, bodyPart.contentType, bodyPart.inputStream))
          }
          i++
        }
        multipartRequest.headers(mapHeaders(request, true))
      } else {
        MockMvcRequestBuilders.request(HttpMethod.valueOf(request.method), requestUriString(request))
          .headers(mapHeaders(request, true))
          .content(body.value)
      }
    } else {
      MockMvcRequestBuilders.request(HttpMethod.valueOf(request.method), requestUriString(request))
        .headers(mapHeaders(request, false))
    }

    executeRequestFilter(requestBuilder, provider)

    return performRequest(mockMvc, requestBuilder).andDo {
      if (debugRequestResponse) {
        MockMvcResultHandlers.print().handle(it)
      }
    }.andReturn()
  }

  private fun executeRequestFilter(requestBuilder: MockHttpServletRequestBuilder, provider: ProviderInfo) {
    val requestFilter = provider.requestFilter
    if (requestFilter != null) {
      when (requestFilter) {
        is Closure<*> -> requestFilter.call(requestBuilder)
        is org.apache.commons.collections4.Closure<*> ->
          (requestFilter as org.apache.commons.collections4.Closure<Any>).execute(requestBuilder)
        else -> {
          if (ProviderClient.isFunctionalInterface(requestFilter)) {
            invokeJavaFunctionalInterface(requestFilter, requestBuilder)
          } else {
            val binding = Binding()
            binding.setVariable(ProviderClient.REQUEST, requestBuilder)
            val shell = GroovyShell(binding)
            shell.evaluate(requestFilter as String)
          }
        }
      }
    }
  }

  private fun invokeJavaFunctionalInterface(functionalInterface: Any, requestBuilder: MockHttpServletRequestBuilder) {
    when (functionalInterface) {
      is Consumer<*> -> (functionalInterface as Consumer<MockHttpServletRequestBuilder>).accept(requestBuilder)
      is Function<*, *> -> (functionalInterface as Function<MockHttpServletRequestBuilder, Any?>).apply(requestBuilder)
      is Callable<*> -> (functionalInterface as Callable<MockHttpServletRequestBuilder>).call()
      else -> throw IllegalArgumentException("Java request filters must be either a Consumer or Function that " +
        "takes at least one MockHttpServletRequestBuilder parameter")
    }
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

  fun requestUriString(request: IRequest): URI {
    val uriBuilder = UriComponentsBuilder.fromPath(request.path)

    val query = request.query
    if (query.isNotEmpty()) {
      query.forEach { (key, value) ->
        uriBuilder.queryParam(key, *value.toTypedArray())
      }
    }

    return URI.create(uriBuilder.toUriString())
  }

  fun mapHeaders(request: IRequest, hasBody: Boolean): HttpHeaders {
    val httpHeaders = HttpHeaders()

    request.headers.forEach { (k, v) ->
      httpHeaders.add(k, v.joinToString(", "))
    }

    if (hasBody && !httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
      httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    }

    return httpHeaders
  }

  fun handleResponse(httpResponse: MockHttpServletResponse): ProviderResponse {
    logger.debug { "Received response: ${httpResponse.status}" }

    val headers = mutableMapOf<String, List<String>>()
    httpResponse.headerNames.forEach { headerName ->
      headers[headerName] = listOf(httpResponse.getHeader(headerName))
    }

    val contentType = if (httpResponse.contentType.isNullOrEmpty()) {
      ContentType.JSON
    } else {
      ContentType.fromString(httpResponse.contentType.toString())
    }

    val response = ProviderResponse(httpResponse.status, headers, contentType,
      OptionalBody.body(httpResponse.contentAsString, contentType))

    logger.debug { "Response: $response" }

    return response
  }

  companion object : KLogging()
}
