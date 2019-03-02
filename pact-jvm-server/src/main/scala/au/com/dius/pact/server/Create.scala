package au.com.dius.pact.server

import au.com.dius.pact.consumer.DefaultMockProvider
import au.com.dius.pact.model._
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._

object Create extends StrictLogging {

  def create(state: String, path: List[String], requestBody: String, oldState: ServerState, config: Config): Result = {
    val pact = PactReader.loadPact(requestBody).asInstanceOf[RequestResponsePact]

    val mockConfig : MockProviderConfig = {
      if(!config.keystorePath.isEmpty) {

        MockHttpsKeystoreProviderConfig
          .httpsKeystoreConfig(config.host, config.sslPort, config.keystorePath, config.keystorePassword,
            PactSpecVersion.fromInt(config.pactVersion))
      }
      else {
        MockProviderConfig.create(config.host, config.portLowerBound, config.portUpperBound,
          PactSpecVersion.fromInt(config.pactVersion))
      }
    }
    val server = DefaultMockProvider.apply(mockConfig)

    val port = server.config.getPort
    val portEntry = port.toString -> server

    // Not very scala...
    val newState = (oldState + portEntry) ++
      (for (
        pathValue <- path
      ) yield (pathValue -> server))

    val body = OptionalBody.body(("{\"port\": " + port + "}").getBytes)

    server.start(pact)

    Result(new Response(201, (ResponseUtils.CrossSiteHeaders ++ Map("Content-Type" -> List("application/json").asJava)).asJava, body), newState)
  }

  def apply(request: Request, oldState: ServerState, config: Config): Result = {
    def errorJson = OptionalBody.body("{\"error\": \"please provide state param and path param and pact body\"}".getBytes)
    def clientError = Result(new Response(400, ResponseUtils.CrossSiteHeaders.asJava, errorJson),
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
}
