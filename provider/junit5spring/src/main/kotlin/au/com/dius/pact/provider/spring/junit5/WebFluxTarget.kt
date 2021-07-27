package au.com.dius.pact.provider.spring.junit5

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.IRequest
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.SynchronousRequestResponse
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderResponse
import au.com.dius.pact.provider.junit5.TestTarget
import org.apache.commons.lang3.StringUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.util.UriComponentsBuilder
import javax.mail.internet.ContentDisposition
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

class WebFluxTarget(private val routerFunction: RouterFunction<*>) : TestTarget {
  override fun getProviderInfo(serviceName: String, pactSource: PactSource?) = ProviderInfo(serviceName)

  override fun prepareRequest(interaction: Interaction, context: MutableMap<String, Any>): Pair<WebTestClient.RequestHeadersSpec<*>, WebTestClient> {
    if (interaction is SynchronousRequestResponse) {
      val request = interaction.request.generatedRequest(context, GeneratorTestMode.Provider)
      val webClient = WebTestClient.bindToRouterFunction(routerFunction).build()
      return toWebFluxRequestBuilder(webClient, request) to webClient
    }
    throw UnsupportedOperationException("Only request/response interactions can be used with an MockMvc test target")
  }

  private fun toWebFluxRequestBuilder(webClient: WebTestClient, request: IRequest): WebTestClient.RequestHeadersSpec<*> {
    return if (request.body.isPresent()) {
      if (request.isMultipartFileUpload()) {
        val multipart = MimeMultipart(ByteArrayDataSource(request.body.unwrap(), request.contentTypeHeader()))

        val bodyBuilder = MultipartBodyBuilder()
        var i = 0
        while (i < multipart.count) {
          val bodyPart = multipart.getBodyPart(i)
          val contentDisposition = ContentDisposition(bodyPart.getHeader("Content-Disposition").first())
          val name = StringUtils.defaultString(contentDisposition.getParameter("name"), "file")
          val filename = contentDisposition.getParameter("filename").orEmpty()

          bodyBuilder
            .part(name, bodyPart.content)
            .filename(filename)
            .contentType(MediaType.valueOf(bodyPart.contentType))
            .header("Content-Disposition", "form-data; name=$name; filename=$filename")

          i++
        }

        webClient
          .method(HttpMethod.POST)
          .uri(requestUriString(request))
          .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
          .headers { request.headers.forEach { (k, v) -> it.addAll(k, v) } }
      } else {
        webClient
          .method(HttpMethod.valueOf(request.method))
          .uri(requestUriString(request))
          .bodyValue(request.body.value!!)
          .headers {
            request.headers.forEach { (k, v) -> it.addAll(k, v) }
            if (!request.headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
              it.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            }
          }
      }
    } else {
      webClient
        .method(HttpMethod.valueOf(request.method))
        .uri(requestUriString(request))
        .headers {
          request.headers.forEach { (k, v) -> it.addAll(k, v) }
        }
    }
  }

  private fun requestUriString(request: IRequest): String {
    val uriBuilder = UriComponentsBuilder.fromPath(request.path)

    request.query.forEach { (key, value) ->
      uriBuilder.queryParam(key, value)
    }

    return uriBuilder.toUriString()
  }

  override fun isHttpTarget() = true

  override fun executeInteraction(client: Any?, request: Any?): ProviderResponse {
    val requestBuilder = request as WebTestClient.RequestHeadersSpec<*>
    val exchangeResult = requestBuilder.exchange().expectBody().returnResult()

    val headers = mutableMapOf<String, List<String>>()
    exchangeResult.responseHeaders.forEach { header ->
      headers[header.key] = header.value
    }

    val contentTypeHeader = exchangeResult.responseHeaders.contentType
    val contentType = if (contentTypeHeader == null) {
      ContentType.JSON
    } else {
      ContentType.fromString(contentTypeHeader.toString())
    }

    return ProviderResponse(
      exchangeResult.status.value(),
      headers,
      contentType,
      exchangeResult.responseBody?.let { String(it) }
    )
  }

  override fun prepareVerifier(verifier: IProviderVerifier, testInstance: Any) {
    /* NO-OP */
  }
}
