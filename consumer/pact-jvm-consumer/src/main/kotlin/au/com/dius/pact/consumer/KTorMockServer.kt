package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.model.MockHttpsProviderConfig
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.receiveText
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import mu.KLogging
import java.util.concurrent.TimeUnit

class KTorMockServer(
  pact: RequestResponsePact,
  config: MockProviderConfig,
  private val stopTimeout: Long = 20000
) : BaseMockServer(pact, config) {

  private val env = applicationEngineEnvironment {
    if (config is MockHttpsProviderConfig) {
      sslConnector(keyStore = config.keyStore!!, keyAlias = config.keyStoreAlias,
        keyStorePassword = { config.keystorePassword.toCharArray() },
        privateKeyPassword = { config.privateKeyPassword.toCharArray() }) {
        host = config.hostname
        port = config.port
//        keyStorePath = config.keyStore.
      }
    } else {
      connector {
        host = config.hostname
        port = config.port
      }
    }

    module {
      install(CallLogging)
      intercept(ApplicationCallPipeline.Call) {
        if (context.request.httpMethod == HttpMethod.Options && context.request.headers.contains("X-PACT-BOOTCHECK")) {
          context.response.header("X-PACT-BOOTCHECK", "true")
          context.respond(HttpStatusCode.OK)
        } else {
          try {
            val request = toPactRequest(context)
            logger.debug { "Received request: $request" }
            val response = generatePactResponse(request)
            logger.debug { "Generating response: $response" }
            pactResponseToKTorResponse(response, context)
          } catch (e: Exception) {
            logger.error(e) { "Failed to generate response" }
            pactResponseToKTorResponse(Response(500, mutableMapOf("Content-Type" to listOf("application/json")),
              OptionalBody.body("{\"error\": ${e.message}}".toByteArray(), ContentType.JSON)), context)
          }
        }
      }
    }
  }

  private var server: ApplicationEngine = embeddedServer(Netty, environment = env, configure = {})

  private suspend fun pactResponseToKTorResponse(response: Response, call: ApplicationCall) {
    response.headers?.forEach { entry ->
      entry.value.forEach {
        call.response.headers.append(entry.key, it, safeOnly = false)
      }
    }

    val body = response.body
    if (body.isPresent()) {
      call.respondBytes(status = HttpStatusCode.fromValue(response.status), bytes = body.unwrap())
    } else {
      call.respond(HttpStatusCode.fromValue(response.status))
    }
  }

  private suspend fun toPactRequest(call: ApplicationCall): Request {
    val headers = call.request.headers
    val bodyContents = call.receiveText()
    val body = if (bodyContents.isEmpty()) {
      OptionalBody.empty()
    } else {
      OptionalBody.body(bodyContents.toByteArray(), ContentType(headers["Content-Type"] ?: ContentType.JSON.contentType))
    }
    return Request(call.request.httpMethod.value, call.request.path(),
      call.request.queryParameters.entries().associate { it.toPair() }.toMutableMap(),
      headers.entries().associate { it.toPair() }.toMutableMap(), body)
  }

  override fun getUrl() =
    "${config.scheme}://${server.environment.connectors.first().host}:${this.getPort()}"

  override fun getPort() = server.environment.connectors.first().port

  override fun start() {
    logger.debug { "Starting mock server" }
    server.start()
    logger.debug { "Mock server started: ${server.environment.connectors}" }
  }

  override fun stop() {
    server.stop(100, stopTimeout, TimeUnit.MILLISECONDS)
    logger.debug { "Mock server shutdown" }
  }

  companion object : KLogging()
}
