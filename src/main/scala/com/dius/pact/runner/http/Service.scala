package com.dius.pact.runner.http

import spray.http.{HttpEntity, HttpRequest, HttpHeader, HttpResponse}
import spray.can.Http
import akka.actor.{ActorSystem, Actor, ActorLogging}
import com.dius.pact.model.Request
import com.dius.pact.runner.RequestMatcher
import spray.http.HttpHeaders.RawHeader
import scala.concurrent.Promise
import scala.util.Try

class Service extends Actor with ActorLogging {
  val requestMatcher = Promise[RequestMatcher]()
  implicit val system = ActorSystem()

  def receive = {

    //singleton request handler, no fancy routing
    case _: Http.Connected => sender ! Http.Register(self)

    case r: HttpRequest => {

      val request = Request(
          r.method.toString().toLowerCase,
          r.uri.path.toString(),
          Some(r.headers.map(HttpHeader.unapply).flatten.toMap),
          Some(r.entity.asString)
      )
      log.debug(s"received $request on ${r.uri}")

      import system.dispatcher
      requestMatcher.future.map {matcher =>
        val response = matcher.responseFor(request)

        val body = response.body.fold[HttpEntity](HttpEntity.Empty)(HttpEntity.apply)
        val headers = response.headers.fold[List[HttpHeader]](Nil)(_.map{ case (k,v) => RawHeader(k,v) }.toList)

        sender ! HttpResponse(status = response.status, entity = body, headers = headers)
      }
    }

    case m: RequestMatcher => requestMatcher.complete(Try(m))

    case m => log.warning(s"not sure what to do with $m")
  }
}