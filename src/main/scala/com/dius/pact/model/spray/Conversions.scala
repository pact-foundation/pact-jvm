package com.dius.pact.model.spray

import com.dius.pact.model.{Response, Request}
import spray.http._
import spray.http.HttpHeaders.RawHeader

object Conversions {
  implicit def toHeaders(from:Option[Map[String,String]]):List[HttpHeader] = {
    from.fold[List[HttpHeader]](Nil)(_.map{ case (k,v) => RawHeader(k,v) }.toList)
  }

  implicit def fromHeaders(from:List[HttpHeader]):Option[Map[String,String]] = {
    from match {
      case Nil => None
      case headers => Some(headers.map(HttpHeader.unapply).flatten.toMap)
    }
  }

  val JSON = ContentTypes.`application/json`

  implicit def toEntity(body:Option[String]):HttpEntity =
    body.fold[HttpEntity](HttpEntity.Empty)(HttpEntity(JSON, _))

  implicit def fromEntity(entity:HttpEntity):Option[String] = {
    entity match {
      case HttpEntity.Empty => None
      case e => Some(e.asString)
    }
  }

  implicit def playToSprayRequest(request:Request):HttpRequest = HttpRequest(
    method = HttpMethods.getForKey(request.method).get,
    uri = Uri(request.path),
    headers = request.headers,
    entity = request.body
  )

  implicit def sprayToPlayRequest(request:HttpRequest):Request = Request(
    method = request.method.value,
    path = request.uri.path.toString(),
    headers = request.headers,
    body = request.entity
  )

  implicit def playToSprayResponse(response:Response):HttpResponse = HttpResponse(
    status = StatusCodes.getForKey(response.status).get,
    headers = response.headers,
    entity = response.body
  )

  implicit def sprayToPlayResponse(response:HttpResponse):Response = Response(
    status = response.status.intValue,
    headers = response.headers,
    body = response.entity
  )
}
