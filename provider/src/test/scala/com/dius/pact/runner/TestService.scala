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
  def receive = awaitState orElse {
    case r:HttpRequest => {
      log.error(s"received unexpected request: $r\n expected only POST to ${Path("/enterState")}")
    }
  }

  def awaitState: Receive = {
    //singleton request handler, no fancy routing
    case Http.Connected(_, _) => sender ! Http.Register(self)

    case HttpRequest(HttpMethods.POST, Uri(_, _, Path("/enterState"), _, _), _, entity, _) => {
      log.info(s"received state transition message ${entity.asString}")
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
      log.info(s"received request: $method $uri : ${entity.asString}")
      val requestBody: Option[JValue] = entity match {
        case HttpEntity.Empty => None
        case e => Some(parse(e.asString))
      }
      val reqHeaders = Some(requestHeaders.map(HttpHeader.unapply).flatten.toMap)
      val request = Request(
        method.value,
        uri.path.toString().replace("http://localhost:8888", ""),
        reqHeaders,
        requestBody
      )
      val response = responses(state)(request.path)
      val body = response.bodyString.fold[HttpEntity](HttpEntity.Empty){content => HttpEntity(ContentTypes.`application/json`, content)}
      val headers = response.headers.fold[List[HttpHeader]](Nil)(_.map{ case (k,v) => RawHeader(k,v) }.toList)

      val sprayResponse = HttpResponse(status = response.status, entity = body, headers = headers)
      sender ! sprayResponse
    }
  }
}
