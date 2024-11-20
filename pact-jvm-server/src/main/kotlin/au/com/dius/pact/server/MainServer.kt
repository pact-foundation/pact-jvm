package au.com.dius.pact.server

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.IResponse
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.install
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveStream
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream

data class ServerStateStore(var state: ServerState = ServerState())

class MainServer(val store: ServerStateStore, val serverConfig: Config) {
  private val env = applicationEngineEnvironment {
    connector {
      host = serverConfig.host
      port = serverConfig.port
    }

    module {
      install(CallLogging)
      intercept(ApplicationCallPipeline.Call) {
        val request = toPactRequest(context)
        val result = RequestRouter.dispatch(request, store.state, serverConfig)
        store.state = result.newState
        pactResponseToKTorResponse(result.response, context)
      }
    }
  }

  val server = embeddedServer(Netty, environment = env, configure = {})

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

  companion object {
    private val COMPRESSED_ENCODINGS = setOf("gzip", "deflate")
  }
}
