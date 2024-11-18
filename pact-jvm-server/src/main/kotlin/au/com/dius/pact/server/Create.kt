package au.com.dius.pact.server

import au.com.dius.pact.consumer.model.MockHttpsProviderConfig
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.support.isNotEmpty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.RandomUtils
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.security.KeyStore

private val logger = KotlinLogging.logger {}

object Create {

  private val CrossSiteHeaders = mapOf("Access-Control-Allow-Origin" to listOf("*"))

  @JvmStatic
  fun create(_state: String, path: List<String?>?, requestBody: String, oldState: ServerState, config: Config): Result {
    val pact = DefaultPactReader.loadPact(requestBody)

    val mockConfig : MockProviderConfig = if (config.keystorePath.isNotEmpty()) {
      val ks = KeyStore.getInstance(File(config.keystorePath), config.keystorePassword.toCharArray())
      MockHttpsProviderConfig(
        config.host,
        config.sslPort,
        PactSpecVersion.fromInt(config.pactVersion),
        ks,
        "localhost",
        config.keystorePassword,
        config.keystorePassword
      )
    }
    else {
      MockProviderConfig(config.host, randomPort(config.portLowerBound, config.portUpperBound),
        PactSpecVersion.fromInt(config.pactVersion))
    }
    val server = DefaultMockProvider.apply(mockConfig)

    val port = server.config.port
    val portEntry = port.toString() to server

    val newState = (oldState.state.entries.map { it.toPair() } + portEntry).toMutableSet()
    if (path != null) {
      for (p in path) {
        if (p != null) {
          newState += (p to server)
        }
      }
    }

    val body = OptionalBody.body(("{\"port\": $port}").toByteArray())

    server.start(pact)

    val headers = CrossSiteHeaders + mapOf("Content-Type" to listOf("application/json"))
    return Result(Response(201, headers.toMutableMap(), body), ServerState(newState.associate { it }))
  }

  @JvmStatic
  fun apply(request: Request, oldState: ServerState, config: Config): Result {
    logger.debug { "path=${request.path}" }
    logger.debug { "query=${request.query}" }
    logger.debug { "body=${request.body}" }

    if (request.query.isNotEmpty()) {
      val stateList = request.query["state"]
      if (stateList != null) {
        val state = stateList.first()
        if (state != null) {
          val paths = request.query["path"]
          if (request.body.isPresent()) {
            return create(state, paths, request.body.valueAsString(), oldState, config)
          }
        }
      }
    }

    val errorJson = OptionalBody.body("{\"error\": \"please provide state param and path param and pact body\"}".toByteArray())
    return Result(Response(400, CrossSiteHeaders.toMutableMap(), errorJson), oldState)
  }

  private fun randomPort(lower: Int, upper: Int): Int {
    var port: Int? = null
    var count = 0
    while (port == null && count < 20) {
      val randomPort = RandomUtils.nextInt(lower, upper)
      if (portAvailable(randomPort)) {
        port = randomPort
      }
      count += 1
    }

    if (port == null) {
      port = 0
    }

    return port
  }

  private fun portAvailable(p: Int): Boolean {
    var socket: ServerSocket? = null
    return try {
      socket = ServerSocket(p)
      true
    } catch (_: IOException) {
      false
    } finally {
      if (socket != null) {
        try {
          socket.close()
        } catch (_: IOException) {
        }
      }
    }
  }
}
