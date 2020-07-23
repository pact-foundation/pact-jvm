package au.com.dius.pact.provider.spring

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderResponse
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.VerificationFailureType
import au.com.dius.pact.provider.VerificationResult
import groovy.lang.Binding
import groovy.lang.Closure
import groovy.lang.GroovyShell
import org.apache.commons.lang3.StringUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.util.UriComponentsBuilder
import scala.Function1
import java.util.concurrent.Callable
import java.util.function.Consumer
import java.util.function.Function
import javax.mail.internet.ContentDisposition
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

class WebFluxProviderVerifier : ProviderVerifier() {

  fun verifyResponseFromProvider(
    provider: ProviderInfo,
    interaction: RequestResponseInteraction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    webClient: WebTestClient,
    pending: Boolean
  ): VerificationResult {
    return try {
      val request = interaction.request

      val clientResponse = executeWebFluxRequest(webClient, request, provider)

      val expectedResponse = interaction.response
      val actualResponse = handleResponse(clientResponse)

      verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, failures,
        interaction.interactionId.orEmpty(), false)
    } catch (e: Exception) {
      logger.error(e) { "Request to provider method failed" }

      failures[interactionMessage] = e
      reporters.forEach {
        it.requestFailed(
          provider, interaction, interactionMessage,
          e, projectHasProperty.apply(PACT_SHOW_STACKTRACE)
        )
      }
      return VerificationResult.Failed(
        listOf(mapOf("message" to "Request to provider method failed with an exception", "exception" to e)),
        "Request to provider method failed with an exception", interactionMessage,
        listOf(VerificationFailureType.ExceptionFailure(e)), pending, interaction.interactionId)
    }
  }

  fun executeWebFluxRequest(
    webTestClient: WebTestClient,
    request: Request,
    provider: ProviderInfo
  ): EntityExchangeResult<ByteArray> {
    val body = request.body

    val builder = if (body.isPresent()) {
      if (request.isMultipartFileUpload()) {
        val multipart = MimeMultipart(ByteArrayDataSource(body.unwrap(), request.contentTypeHeader()))

        val bodyBuilder = MultipartBodyBuilder()
        var i = 0
        while (i < multipart.count) {
          val bodyPart = multipart.getBodyPart(i)
          val contentDisposition = ContentDisposition(bodyPart.getHeader("Content-Disposition").first())
          val name = StringUtils.defaultString(contentDisposition.getParameter("name"), "file")
          val filename = contentDisposition.getParameter("filename").orEmpty()

          bodyBuilder
            .part(name, bodyPart.content)
            .header("Content-Disposition", "form-data; name=$name; filename=$filename")

          i++
        }

        webTestClient
          .method(HttpMethod.POST)
          .uri(requestUriString(request))
          .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
          .headers { request.headers.forEach { k, v -> it.addAll(k, v) } }
      } else {
        webTestClient
          .method(HttpMethod.valueOf(request.method))
          .uri(requestUriString(request))
          .bodyValue(body.value!!)
          .headers {
            request.headers.forEach { k, v -> it.addAll(k, v) }
            if (!request.headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
              it.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            }
          }
      }
    } else {
      webTestClient
        .method(HttpMethod.valueOf(request.method))
        .uri(requestUriString(request))
        .headers {
          request.headers.forEach { k, v -> it.addAll(k, v) }
        }
    }

    executeRequestFilter(builder, provider)

    val clientResponse = builder.exchange()

    return clientResponse.expectBody().returnResult()
  }

  private fun executeRequestFilter(requestBuilder: WebTestClient.RequestHeadersSpec<*>, provider: ProviderInfo) {
    val requestFilter = provider.requestFilter
    if (requestFilter != null) {
      when (requestFilter) {
        is Closure<*> -> requestFilter.call(requestBuilder)
        is Function1<*, *> ->
          (requestFilter as Function1<WebTestClient.RequestHeadersSpec<*>, *>).apply(requestBuilder)
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

  private fun invokeJavaFunctionalInterface(
    functionalInterface: Any,
    headersSpec: WebTestClient.RequestHeadersSpec<*>
  ) {
    when (functionalInterface) {
      is Consumer<*> ->
        (functionalInterface as Consumer<WebTestClient.RequestHeadersSpec<*>>).accept(headersSpec)
      is Function<*, *> ->
        (functionalInterface as Function<WebTestClient.RequestHeadersSpec<*>, Any?>).apply(headersSpec)
      is Callable<*> ->
        (functionalInterface as Callable<WebTestClient.RequestHeadersSpec<*>>).call()
      else -> throw IllegalArgumentException(
        "Java request filters must be either a Consumer or Function that " +
          "takes at least one WebTestClient.RequestHeadersSpec<*> parameter")
    }
  }

  fun requestUriString(request: Request): String {
    val uriBuilder = UriComponentsBuilder.fromPath(request.path)

    request.query.forEach { (key, value) ->
      uriBuilder.queryParam(key, value)
    }

    return uriBuilder.toUriString()
  }

  fun handleResponse(exchangeResult: EntityExchangeResult<ByteArray>): ProviderResponse {
    logger.debug { "Received response: ${exchangeResult.status}" }

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

    val response = ProviderResponse(
      exchangeResult.status.value(),
      headers,
      contentType,
      exchangeResult.responseBody?.let { String(it) }
    )

    logger.debug { "Response: $response" }

    return response
  }
}
