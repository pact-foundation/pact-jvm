package au.com.dius.pact.model.spray

import au.com.dius.pact.model.{Response, Request}
import spray.http._
import spray.http.HttpHeaders.RawHeader
import org.json4s._
import org.json4s.jackson.JsonMethods._

object Conversions {
  implicit def toHeaders(from: Option[Map[String,String]]): List[HttpHeader] = {
    from.fold[List[HttpHeader]](Nil)(_.map{ case (k,v) => RawHeader(k,v) }.toList)
  }

  implicit def fromHeaders(from: List[HttpHeader]):Option[Map[String,String]] = {
    from match {
      case Nil => None
      case headers => Some(headers.map(HttpHeader.unapply).flatten.toMap)
    }
  }

  val JSON = ContentTypes.`application/json`

  implicit def toEntity(body: Option[JValue]): HttpEntity = {
    body.fold[HttpEntity](HttpEntity.Empty){ e =>
      val json = compact(render(e))
      HttpEntity(JSON, json)
    }
  }

  implicit def fromEntity(entity: HttpEntity): Option[JValue] = {
    entity match {
      case HttpEntity.Empty => None
      case e => Some(parse(e.asString))
    }
  }

  implicit def pactToSprayRequest(request: Request): HttpRequest = HttpRequest(
    method = HttpMethods.getForKey(request.method).get,
    uri = Uri(request.path),
    headers = request.headers,
    entity = request.body
  )

  implicit def sprayToPactRequest(request: HttpRequest): Request = Request(
    method = request.method.value.toUpperCase,
    path = request.uri.path.toString(),
    headers = request.headers,
    body = request.entity
  )

  implicit def pactToSprayResponse(response: Response): HttpResponse = HttpResponse(
    status = StatusCodes.getForKey(response.status).get,
    headers = response.headers,
    entity = response.body
  )

  implicit def sprayToPactResponse(response: HttpResponse): Response = Response(
    status = response.status.intValue,
    headers = response.headers,
    body = response.entity
  )
}
