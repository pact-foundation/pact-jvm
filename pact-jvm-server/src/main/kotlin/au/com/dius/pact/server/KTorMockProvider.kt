package au.com.dius.pact.server

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.IResponse
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.Response
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.install
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveStream
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream

private val logger = KotlinLogging.logger {}

abstract class BaseKTorMockProvider(override val config: MockProviderConfig): StatefulMockProvider() {

  lateinit var server: NettyApplicationEngine

  suspend fun toPactRequest(call: ApplicationCall): Request {
    val request = call.request
    val headers = request.headers
    val bodyContents = withContext(Dispatchers.IO) {
      val stream = call.receiveStream()
      when (bodyIsCompressed(headers["Content-Encoding"])) {
        "gzip" -> GZIPInputStream(stream).readBytes()
        "deflate" -> DeflaterInputStream(stream).readBytes()
        else -> stream.readBytes()
      }
    }
    val body = if (bodyContents.isEmpty()) {
      OptionalBody.empty()
    } else {
      OptionalBody.body(bodyContents, ContentType.fromString(headers["Content-Type"]).or(ContentType.JSON))
    }
    return Request(request.httpMethod.value, request.path(),
      request.queryParameters.entries().associate { it.toPair() }.toMutableMap(),
      headers.entries().associate { it.toPair() }.toMutableMap(), body)
  }

  private fun bodyIsCompressed(encoding: String?): String? {
    return if (COMPRESSED_ENCODINGS.contains(encoding)) encoding else null
  }

  suspend fun pactResponseToKTorResponse(response: IResponse, call: ApplicationCall) {
    response.headers.forEach { entry ->
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

  override fun start() {
    logger.debug { "Starting mock server" }
    server.start()
    logger.debug { "Mock server started: ${server.environment.connectors}" }
  }

  override fun stop() {
    server.stop(100)
    logger.debug { "Mock server shutdown" }
  }

  companion object {
    private val COMPRESSED_ENCODINGS = setOf("gzip", "deflate")
  }
}

class KTorMockProvider(override val config: MockProviderConfig): BaseKTorMockProvider(config) {
  private val serverHostname = config.hostname
  private val serverPort = config.port

  private val env = applicationEngineEnvironment {
    connector {
      host = serverHostname
      port = serverPort
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
            val response = handleRequest(request)
            pactResponseToKTorResponse(response, context)
          } catch (e: Exception) {
            logger.error(e) { "Failed to generate response" }
            pactResponseToKTorResponse(
              Response(500, mutableMapOf("Content-Type" to listOf("application/json")),
                OptionalBody.body("{\"error\": ${e.message}}".toByteArray(), ContentType.JSON)), context)
          }
        }
      }
    }
  }

  init {
    server = embeddedServer(Netty, environment = env, configure = {})
  }
}
