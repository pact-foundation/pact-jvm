package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.model.MockHttpsProviderConfig
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.IResponse
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.support.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.install
import io.ktor.server.application.serverConfig
import io.ktor.server.engine.EngineConnectorConfig
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveStream
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream

private val logger = KotlinLogging.logger {}

class KTorMockServer @JvmOverloads constructor(
  pact: BasePact,
  config: MockProviderConfig,
  private val stopTimeout: Long = 20000
) : BaseMockServer(pact, config) {
  private var localAddress: EngineConnectorConfig? = null

  private val serverProperties = serverConfig {
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
  private var server = embeddedServer(Netty, serverProperties) {
    if (config is MockHttpsProviderConfig) {
      sslConnector(keyStore = config.keyStore!!, keyAlias = config.keyStoreAlias,
        keyStorePassword = { config.keystorePassword.toCharArray() },
        privateKeyPassword = { config.privateKeyPassword.toCharArray() }) {
        host = config.hostname
        port = config.port
      }
    } else {
      connector {
        host = config.hostname
        port = config.port
      }
    }
  }

  private suspend fun pactResponseToKTorResponse(response: IResponse, call: ApplicationCall) {
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

  private suspend fun toPactRequest(call: ApplicationCall): Request {
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

  override fun getUrl(): String {
    val connectorConfig = server.engine.configuration.connectors.first()
    return if (localAddress != null) {
      // Stupid GitHub Windows agents
      val host = if (localAddress!!.host.lowercase() == "miningmadness.com") {
        connectorConfig.host
      } else {
        localAddress!!.host
      }
      URI(config.scheme, null, host, localAddress!!.port, null, null, null).toString()
    } else {
      URI(config.scheme, null, connectorConfig.host, connectorConfig.port, null, null, null).toString()
    }
  }

  override fun getPort(): Int {
    return if (localAddress != null) {
      localAddress!!.port
    } else {
      val connectorConfig = server.engine.configuration.connectors.first()
      connectorConfig.port
    }
  }

  override fun updatePact(pact: Pact): Pact {
    return if (pact.isV4Pact()) {
      when (val p = pact.asV4Pact()) {
        is Result.Ok -> {
          for (interaction in p.value.interactions) {
            interaction.asV4Interaction().transport = if (config is MockHttpsProviderConfig) "https" else "http"
          }
          p.value
        }
        is Result.Err -> pact
      }
    } else {
      pact
    }
  }

  override fun start() {
    logger.debug { "Starting mock server" }

    CoroutineScope(server.application.coroutineContext).launch {
      localAddress = server.engine.resolvedConnectors().first()
    }

    server.start()
    logger.debug { "Mock server started: $localAddress" }
  }

  override fun stop() {
    server.stop(100, stopTimeout)
    logger.debug { "Mock server shutdown" }
  }
}
