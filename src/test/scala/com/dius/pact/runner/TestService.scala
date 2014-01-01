package com.dius.pact.runner

import spray.http._
import spray.can.Http
import akka.actor.{ActorSystem, Actor, ActorLogging}
import spray.http.HttpRequest
import spray.http.HttpResponse
import spray.http.HttpHeaders.RawHeader
import com.dius.pact.model.Request
import scala.Some
import AnimalServiceResponses.responses
import spray.http.Uri.{Query, Path, Authority}
import play.api.libs.json.Json

class TestService extends Actor with ActorLogging {

  implicit val system = ActorSystem()
  var state = "none"

  def receive = {

    //singleton request handler, no fancy routing
    case Http.Connected(_, _) => sender ! Http.Register(self)
    case HttpRequest(HttpMethods.POST, Uri(_, _, Path("/setup"), _, _), _, entity, _) => {
      state = (Json.parse(entity.data.asString) \ "state").as[String]
      println(s"entered state: $state")
      sender ! HttpResponse(status = 200)
    }

    case HttpRequest(method, uri, headers, entity, protocol) => {
      val request = Request(
        com.dius.pact.model.HttpMethod.build(method.value),
        uri.path.toString().replace("http://localhost:8888", ""),
        Some(headers.map(HttpHeader.unapply).flatten.toMap),
        Some(entity.asString)
      )
      log.info(s"received $request on $uri")

      {
        val response = responses(state)(request.path)
        val body = response.body.fold[HttpEntity](HttpEntity.Empty){content => HttpEntity(ContentTypes.`application/json`, content)}
        val headers = response.headers.fold[List[HttpHeader]](Nil)(_.map{ case (k,v) => RawHeader(k,v) }.toList)

        sender ! HttpResponse(status = response.status, entity = body, headers = headers)
      }
    }

    case m => log.warning(s"not sure what to do with $m")
  }
}
