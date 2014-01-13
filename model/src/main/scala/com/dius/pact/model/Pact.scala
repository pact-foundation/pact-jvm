package com.dius.pact.model

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

object Pact {
  def from(source: JsonInput): Pact = {
    implicit val formats = DefaultFormats
    parse(source).transformField { case ("provider_state", value) => ("providerState", value)}.extract[Pact]
  }
}

case class Pact(provider:Provider, consumer:Consumer, interactions:Seq[Interaction]) extends PactSerializer {
  def interactionFor(description:String, providerState:String) = interactions.find { i =>
    i.description == description && i.providerState == providerState
  }
}

case class Provider(name:String)

case class Consumer(name:String)

case class Interaction(
                        description: String,
                        providerState: String,
                        request: Request,
                        response: Response)

object HttpMethod {
  val Get    = "GET"
  val Post   = "POST"
  val Put    = "PUT"
  val Delete = "DELETE"
  val Head   = "HEAD"
  val Patch  = "PATCH"
}

//TODO: support duplicate headers
case class Request(method: String,
                   path: String,
                   headers: Option[Map[String, String]],
                   body: Option[JValue]) {
  def bodyString:Option[String] = body.map{ b => compact(render(b))}
}

//TODO: support duplicate headers
case class Response(status: Int,
                    headers: Option[Map[String, String]],
                    body: Option[JValue]) {
  def bodyString:Option[String] = body.map{ b => compact(render(b)) }
}

object Response {
  def apply(status: Int, headers: Map[String, String], body: JValue):Response = {
    val optionalHeaders = if(headers == null || headers.isEmpty) {
      None
    } else {
      Some(headers)
    }
    val optionalBody = if(body == null) {
      None
    } else {
      Some(body)
    }
    Response(status, optionalHeaders, optionalBody)
  }

  def invalidRequest(request: Request, pact: Pact) = {
    Response(500, Map[String, String](), "error"-> s"unexpected request : $request \nnot in : $pact")
  }
}


