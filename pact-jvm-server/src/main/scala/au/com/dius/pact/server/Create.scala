package au.com.dius.pact.server

import au.com.dius.pact.com.typesafe.scalalogging.StrictLogging
import au.com.dius.pact.consumer.DefaultMockProvider
import au.com.dius.pact.model.{MockProviderConfig, Pact, Request, Response}
import org.jboss.netty.handler.codec.http.QueryStringDecoder
import org.json4s.JsonDSL.{int2jvalue, map2jvalue, string2jvalue}
import org.json4s.jackson.JsonMethods.pretty

import scala.collection.JavaConversions._


object Create extends StrictLogging {
  
  def create(state: String, requestBody: String, oldState: ServerState, config: Config): Result = {
    val pact = Pact.from(requestBody)
    val mockConfig: MockProviderConfig = MockProviderConfig.create(config.portLowerBound, config.portUpperBound)
      .copy(hostname = config.host)
    val server = DefaultMockProvider.apply(mockConfig)
    val port = server.config.port
    val entry = port -> server
    val body = pretty(map2jvalue(Map("port" -> port)))

    server.start(pact)

    Result(Response(201, Response.CrossSiteHeaders ++ Map("Content-Type" -> "application/json"), body, null), oldState + entry)
  }

  def apply(request: Request, oldState: ServerState, config: Config): Result = {
    def errorJson = pretty(map2jvalue(Map("error" -> "please provide state param and pact body")))
    def clientError = Result(Response(400, Response.CrossSiteHeaders, errorJson, null), oldState)
    val params = new QueryStringDecoder(s"/?${request.query.getOrElse("")}").getParameters.toMap

    logger.debug(s"path=${request.path}")
    logger.debug(s"query=${request.query.toString}")
    logger.debug(request.body.toString)

    val result = for {
      stateList <- params.get("state")
      state <- stateList.toList.headOption
      body <- request.body
    } yield create(state, body, oldState, config)

    result getOrElse clientError
  }
}
