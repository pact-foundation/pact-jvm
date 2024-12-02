package au.com.dius.pact.server

import au.com.dius.pact.consumer.model.MockHttpsProviderConfig
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Response
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.install
import io.ktor.server.application.serverConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.response.header
import io.ktor.server.response.respond

private val logger = KotlinLogging.logger {}

class KTorHttpsKeystoreMockProvider(override val config: MockHttpsProviderConfig): BaseKTorMockProvider(config) {
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

  override var server = embeddedServer(Netty, serverProperties) {
    sslConnector(keyStore = config.keyStore!!,
      keyAlias = config.keyStoreAlias,
      keyStorePassword = { config.keystorePassword.toCharArray() },
      privateKeyPassword = { config.privateKeyPassword.toCharArray() }) {
      host = config.hostname
      port = config.port
    }
  }
}
