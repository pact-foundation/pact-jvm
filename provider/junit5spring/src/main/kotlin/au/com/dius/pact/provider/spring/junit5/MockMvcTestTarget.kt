package au.com.dius.pact.provider.spring.junit5

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderResponse
import au.com.dius.pact.provider.junit5.TestTarget
import mu.KLogging
import org.apache.commons.lang3.StringUtils
import org.hamcrest.core.IsAnything
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import javax.mail.internet.ContentDisposition
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

/**
 * Test target for tests using Spring MockMvc.
 */
class MockMvcTestTarget @JvmOverloads constructor(
  var mockMvc: MockMvc? = null,
  var controllers: List<Any> = mutableListOf(),
  var controllerAdvices: List<Any> = mutableListOf(),
  var messageConverters: List<HttpMessageConverter<*>> = mutableListOf(),
  var printRequestResponse: Boolean = false,
  var servletPath: String? = null
) : TestTarget {
    override fun getProviderInfo(serviceName: String, pactSource: PactSource?) = ProviderInfo(serviceName)

    override fun prepareRequest(interaction: Interaction, context: MutableMap<String, Any>): Pair<MockHttpServletRequestBuilder, MockMvc> {
      if (interaction is RequestResponseInteraction) {
        return toMockRequestBuilder(interaction.request.generatedRequest(context)) to buildMockMvc()
      }
      throw UnsupportedOperationException("Only request/response interactions can be used with an MockMvc test target")
    }

    fun setControllers(vararg controllers: Any) {
        this.controllers = controllers.asList()
    }

    fun setControllerAdvices(vararg controllerAdvices: Any) {
        this.controllerAdvices = controllerAdvices.asList()
    }

    fun setMessageConverters(vararg messageConverters: HttpMessageConverter<*>) {
        this.messageConverters = messageConverters.asList()
    }

    private fun buildMockMvc(): MockMvc {
        if (mockMvc != null) {
            return mockMvc!!
        }

        val requestBuilder = MockMvcRequestBuilders.get("/")
        if (!servletPath.isNullOrEmpty()) {
            requestBuilder.servletPath(servletPath)
        }

        return MockMvcBuilders.standaloneSetup(*controllers.toTypedArray())
          .setControllerAdvice(*controllerAdvices.toTypedArray())
          .setMessageConverters(*messageConverters.toTypedArray())
          .defaultRequest<StandaloneMockMvcBuilder>(requestBuilder)
          .build()
    }

    private fun toMockRequestBuilder(request: Request): MockHttpServletRequestBuilder {
        val body = request.body
        return if (body.isPresent()) {
            if (request.isMultipartFileUpload()) {
                val multipart = MimeMultipart(ByteArrayDataSource(body.unwrap(), request.contentTypeHeader()))
                val multipartRequest = MockMvcRequestBuilders.fileUpload(requestUriString(request))
                var i = 0
                while (i < multipart.count) {
                    val bodyPart = multipart.getBodyPart(i)
                    val contentDisposition = ContentDisposition(bodyPart.getHeader("Content-Disposition").first())
                    val name = StringUtils.defaultString(contentDisposition.getParameter("name"), "file")
                    val filename = contentDisposition.getParameter("filename").orEmpty()
                    multipartRequest.file(MockMultipartFile(name, filename, bodyPart.contentType, bodyPart.inputStream))
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
    }

    private fun requestUriString(request: Request): URI {
        val uriBuilder = UriComponentsBuilder.fromPath(request.path)

        val query = request.query
        if (query.isNotEmpty()) {
            query.forEach { (key, value) ->
                uriBuilder.queryParam(key, *value.toTypedArray())
            }
        }

        return URI.create(uriBuilder.toUriString())
    }

    private fun mapHeaders(request: Request, hasBody: Boolean): HttpHeaders {
        val httpHeaders = HttpHeaders()

        request.headers.forEach { (k, v) ->
            httpHeaders.add(k, v.joinToString(", "))
        }

        if (hasBody && !httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
            httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        }

        return httpHeaders
    }

    override fun isHttpTarget() = true

    override fun executeInteraction(client: Any?, request: Any?): ProviderResponse {
        val mockMvcClient = client as MockMvc
        val requestBuilder = request as MockHttpServletRequestBuilder
        val mvcResult = performRequest(mockMvcClient, requestBuilder).andDo {
            if (printRequestResponse) {
                MockMvcResultHandlers.print().handle(it)
            }
        }.andReturn()

        return handleResponse(mvcResult.response)
    }

    private fun performRequest(mockMvc: MockMvc, requestBuilder: RequestBuilder): ResultActions {
        val resultActions = mockMvc.perform(requestBuilder)
        return if (resultActions.andReturn().request.isAsyncStarted) {
            mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(resultActions
              .andExpect(MockMvcResultMatchers.request().asyncResult<Any>(IsAnything()))
              .andReturn()))
        } else {
            resultActions
        }
    }

    private fun handleResponse(httpResponse: MockHttpServletResponse): ProviderResponse {
      logger.debug { "Received response: ${httpResponse.status}" }

      val headers = mutableMapOf<String, List<String>>()
      httpResponse.headerNames.forEach { headerName ->
          headers[headerName] = listOf(httpResponse.getHeader(headerName))
      }

      val contentType = if (httpResponse.contentType.isNullOrEmpty()) {
          ContentType.JSON
      } else {
          ContentType.fromString(httpResponse.contentType)
      }

      val response = ProviderResponse(httpResponse.status, headers, contentType, httpResponse.contentAsString)

      logger.debug { "Response: $response" }

      return response
    }

    override fun prepareVerifier(verifier: IProviderVerifier, testInstance: Any) {
        /* NO-OP */
    }

    companion object : KLogging()
}
