package au.com.dius.pact.server

import org.jboss.netty.handler.codec.http.QueryStringDecoder
import org.json4s.JValue
import org.json4s.JsonDSL.int2jvalue
import org.json4s.JsonDSL.pair2jvalue
import org.json4s.JsonDSL.string2jvalue
import au.com.dius.pact.consumer.DefaultMockProvider
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.Response
import scala.collection.JavaConversions._


object Create {
  
  def create(state: String, requestBody: JValue, oldState: ServerState): Result = {
    val pact = Pact.from(requestBody)
    val server = DefaultMockProvider.withDefaultConfig()
    val port = server.config.port
    val entry = port -> server
    val body: JValue = "port" -> port
    Result(Response(201, Response.CrossSiteHeaders, body, null), oldState + entry)
  }

  def apply(request: Request, oldState: ServerState): Result = {
    def errorJson: JValue = "error" -> "please provide state param and pact body"
    def clientError = Result(Response(400, Response.CrossSiteHeaders, errorJson, null), oldState)
    val params = new QueryStringDecoder(request.path).getParameters.toMap
    
    val result = for {
      stateList <- params.get("state")
      state <- stateList.toList.headOption
      jsonPact <- request.body
    } yield create(state, jsonPact, oldState)

    result getOrElse clientError
  }
}
