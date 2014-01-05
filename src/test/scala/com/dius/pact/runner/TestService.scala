package com.dius.pact.runner

import spray.http._
import spray.can.Http
import akka.actor.{Actor, ActorLogging}
import spray.http.HttpRequest
import spray.http.HttpResponse
import spray.http.HttpHeaders.RawHeader
import com.dius.pact.model.Request
import scala.Some
import AnimalServiceResponses.responses
import spray.http.Uri.Path
import org.json4s._
import org.json4s.jackson.JsonMethods._

class TestService extends Actor with ActorLogging {
  def receive = awaitState

  def awaitState: Receive = {
    //singleton request handler, no fancy routing
    case Http.Connected(_, _) => sender ! Http.Register(self)

    case HttpRequest(HttpMethods.POST, Uri(_, _, Path("/setup"), _, _), _, entity, _) => {
      parse(entity.data.asString) match {
        case JObject(List(JField("state", JString(state)))) => {
          context.become(running(state))
          sender ! HttpResponse(status = 200)
        }
        case _ => {
          log.error(s"invalid state posted: $entity")
          sender ! HttpResponse(status = 400, entity = HttpEntity("Invalid body content"))
        }
      }
    }
  }

  def running(state: String): Receive = awaitState orElse {
    case HttpRequest(method, uri, requestHeaders, entity, protocol) => {
      val request = Request(
        com.dius.pact.model.HttpMethod.build(method.value),
        uri.path.toString().replace("http://localhost:8888", ""),
        Some(requestHeaders.map(HttpHeader.unapply).flatten.toMap),
        Some(entity.asString)
      )
      val response = responses(state)(request.path)
      val body = response.body.fold[HttpEntity](HttpEntity.Empty){content => HttpEntity(ContentTypes.`application/json`, content)}
      val headers = response.headers.fold[List[HttpHeader]](Nil)(_.map{ case (k,v) => RawHeader(k,v) }.toList)

      sender ! HttpResponse(status = response.status, entity = body, headers = headers)
    }
  }
}
