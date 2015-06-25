package au.com.dius.pact.server

import au.com.dius.pact.consumer.DefaultMockProvider
import au.com.dius.pact.model.{Pact, Request, Response}
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.jboss.netty.handler.codec.http.QueryStringDecoder
import org.json4s.JValue
import org.json4s.JsonDSL.{int2jvalue, map2jvalue, string2jvalue}
import org.json4s.jackson.JsonMethods.pretty

import scala.collection.JavaConversions._


object Create extends StrictLogging {
  
  def create(state: String, requestBody: JValue, oldState: ServerState): Result = {
    val pact = Pact.from(requestBody)
    val server = DefaultMockProvider.withDefaultConfig()
    val port = server.config.port
    val entry = port -> server
    val body = pretty(map2jvalue(Map("port" -> port)))

    Result(Response(201, Response.CrossSiteHeaders ++ Map("Content-Type" -> "application/json"), body, null), oldState + entry)
  }

  def apply(request: Request, oldState: ServerState): Result = {
    def errorJson = pretty(map2jvalue(Map("error" -> "please provide state param and pact body")))
    def clientError = Result(Response(400, Response.CrossSiteHeaders, errorJson, null), oldState)
    val params = new QueryStringDecoder(request.path).getParameters.toMap

    logger.error(request.path)
    logger.error(request.body.toString)

    val result = for {
      stateList <- params.get("state")
      state <- stateList.toList.headOption
      jsonPact <- request.body
    } yield create(state, jsonPact, oldState)

    result getOrElse clientError
  }
}
