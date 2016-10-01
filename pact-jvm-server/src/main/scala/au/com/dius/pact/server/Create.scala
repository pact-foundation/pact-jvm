package au.com.dius.pact.server

import au.com.dius.pact.com.typesafe.scalalogging.StrictLogging
import au.com.dius.pact.consumer.DefaultMockProvider
import au.com.dius.pact.model._

import scala.collection.JavaConversions

object Create extends StrictLogging {
  
  def create(state: String, requestBody: String, oldState: ServerState, config: Config): Result = {
    val pact = PactReader.loadPact(requestBody).asInstanceOf[RequestResponsePact]
    val mockConfig: MockProviderConfig = MockProviderConfig.create(config.host, config.portLowerBound, config.portUpperBound,
      PactSpecVersion.fromInt(config.pactVersion))
    val server = DefaultMockProvider.apply(mockConfig)
    val port = server.config.getPort
    val entry = port -> server
    val body = OptionalBody.body("{\"port\": " + port + "}")

    server.start(pact)

    Result(new Response(201, JavaConversions.mapAsJavaMap(ResponseUtils.CrossSiteHeaders ++
      Map("Content-Type" -> "application/json")), body), oldState + entry)
  }

  def apply(request: Request, oldState: ServerState, config: Config): Result = {
    def errorJson = OptionalBody.body("{\"error\": \"please provide state param and pact body\"}")
    def clientError = Result(new Response(400, JavaConversions.mapAsJavaMap(ResponseUtils.CrossSiteHeaders), errorJson),
      oldState)

    logger.debug(s"path=${request.getPath}")
    logger.debug(s"query=${request.getQuery}")
    logger.debug(request.getBody.toString)

    val result = if (request.getQuery != null) {
      for {
        stateList <- CollectionUtils.javaLMapToScalaLMap(request.getQuery).get("state")
        state <- stateList.headOption
        body <- Option(request.getBody)
      } yield create(state, body.getValue, oldState, config)
    } else None

    result getOrElse clientError
  }
}
