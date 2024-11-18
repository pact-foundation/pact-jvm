package au.com.dius.pact.server

import java.io.{File, IOException}
import java.net.ServerSocket
import au.com.dius.pact.consumer.model.{MockHttpsProviderConfig, MockProviderConfig}
import au.com.dius.pact.core.model._
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.lang3.RandomUtils

import java.security.KeyStore
import scala.collection.JavaConverters._

object Create extends StrictLogging {

  def create(state: String, path: List[String], requestBody: String, oldState: ServerState, config: Config): Result = {
    val pact = DefaultPactReader.INSTANCE.loadPact(requestBody).asInstanceOf[RequestResponsePact]

    val mockConfig : MockProviderConfig = {
      if (config.getKeystorePath.nonEmpty) {
        val ks = KeyStore.getInstance(new File(config.getKeystorePath), config.getKeystorePassword.toCharArray)
        new MockHttpsProviderConfig(
          config.getHost,
          config.getSslPort,
          PactSpecVersion.fromInt(config.getPactVersion),
          ks,
          "localhost",
          config.getKeystorePassword,
          config.getKeystorePassword
        )
      }
      else {
        new MockProviderConfig(config.getHost, randomPort(config.getPortLowerBound, config.getPortUpperBound),
          PactSpecVersion.fromInt(config.getPactVersion))
      }
    }
    val server = DefaultMockProvider.INSTANCE.apply(mockConfig)

    val port = server.getConfig.getPort
    val portEntry = port.toString -> server

    // Not very scala...
    val newState = (oldState.getState.asScala + portEntry) ++
      (for (
        pathValue <- path
      ) yield (pathValue -> server))

    val body = OptionalBody.body(("{\"port\": " + port + "}").getBytes)

    server.start(pact)

    new Result(new Response(201, (ResponseUtils.CrossSiteHeaders ++ Map("Content-Type" -> List("application/json").asJava)).asJava, body), new ServerState(newState.asJava))
  }

  def apply(request: Request, oldState: ServerState, config: Config): Result = {
    def errorJson = OptionalBody.body("{\"error\": \"please provide state param and path param and pact body\"}".getBytes)
    def clientError = new Result(new Response(400, ResponseUtils.CrossSiteHeaders.asJava, errorJson),
      oldState)

    logger.debug(s"path=${request.getPath}")
    logger.debug(s"query=${request.getQuery}")
    logger.debug(request.getBody.toString)

    val result = if (request.getQuery != null) {
      for {
        stateList <- CollectionUtils.javaLMapToScalaLMap(request.getQuery).get("state")
        state <- stateList.headOption
        paths <- CollectionUtils.javaLMapToScalaLMap(request.getQuery).get("path")
        body <- Option(request.getBody)
      } yield create(state, paths, body.valueAsString(), oldState, config)
    } else None

    result getOrElse clientError
  }

  def randomPort(lower: Int, upper: Int): Int = {
    var port: Integer = null
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

    port
  }

  private def portAvailable(p: Int): Boolean = {
    var socket: ServerSocket = null
    try {
      socket = new ServerSocket(p)
      true
    } catch {
      case _: IOException => false
    } finally {
      if (socket != null) {
        try {
          socket.close()
        } catch {
          case _: IOException => {}
        }
      }
    }
  }
}
