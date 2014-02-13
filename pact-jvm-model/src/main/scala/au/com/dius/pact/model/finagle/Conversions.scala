package au.com.dius.pact.model.finagle

import au.com.dius.pact.model.{Response, Request}

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.jboss.netty.handler.codec.http._

object Conversions {
  implicit def toHeaders(from: Option[Map[String,String]]): HttpHeaders = ???

  implicit def fromHeaders(from: HttpHeaders):Option[Map[String,String]] = ???

  implicit def toEntity(body: Option[JValue]): HttpEntity = ???

  implicit def fromEntity(entity: HttpEntity): Option[JValue] = ???

  implicit def pactToSprayRequest(request: Request): HttpRequest = ???

  implicit def sprayToPactRequest(request: HttpRequest): Request = ???

  implicit def pactToSprayResponse(response: Response): HttpResponse = ???

  implicit def sprayToPactResponse(response: HttpResponse): Response = ???
}
