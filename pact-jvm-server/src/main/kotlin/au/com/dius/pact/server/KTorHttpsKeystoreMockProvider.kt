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
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.response.header
import io.ktor.server.response.respond

private val logger = KotlinLogging.logger {}

class KTorHttpsKeystoreMockProvider(override val config: MockHttpsProviderConfig): BaseKTorMockProvider(config) {
  private val serverHostname = config.hostname
  private val serverPort = config.port
  private val keyStore = config.keyStore!!
  private val keyStoreAlias = config.keyStoreAlias
  private val password = config.keystorePassword
  private val privateKeyPassword = config.privateKeyPassword

  private val env = applicationEngineEnvironment {
    sslConnector(keyStore = keyStore,
      keyAlias = keyStoreAlias,
      keyStorePassword = { password.toCharArray() },
      privateKeyPassword = { privateKeyPassword.toCharArray() }) {
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
