package com.dius.pact.model

import java.io.PrintWriter

case class Pact(provider:Provider, consumer:Consumer, interactions:Seq[Interaction]) {
  def interactionFor(description:String, providerState:String) = interactions.find { i =>
    i.description == description && i.providerState == providerState
  }

  //TODO: serialize pact
  def serialize(writer: PrintWriter) {
    writer.print("woo hoo!")
  }
}

case class Provider(name:String)

case class Consumer(name:String)

case class Interaction(
                        description: String,
                        providerState: String,
                        request:Request,
                        response:Response)

class HttpMethod(value:String) {
  override def toString = value
}

case object Get    extends HttpMethod("GET")
case object Post   extends HttpMethod("POST")
case object Put    extends HttpMethod("PUT")
case object Delete extends HttpMethod("DELETE")
case object Head   extends HttpMethod("HEAD")
case object Patch  extends HttpMethod("PATCH")

object HttpMethod {
  def build(key: String) = {
    key.toUpperCase match {
      case "GET" =>   Get
      case "POST" =>    Post
      case "PUT" =>   Put
      case "DELETE" =>    Delete
      case "HEAD" =>    Head
      case "PATCH" =>   Patch
    }
  }
}
//TODO: support duplicate headers
case class Request(method: HttpMethod,
                   path:String,
                   headers:Option[Map[String, String]],
                   body:Option[String])//TODO: convert body to json



//TODO: support duplicate headers
case class Response(status: Int,
                    headers:Option[Map[String, String]],
                    body:Option[String])//TODO: convert body to json

object Response {
  def apply(status:Int, headers:Map[String, String], body:String):Response = {
    val optionalHeaders = if(headers == null || headers.isEmpty) {
      None
    } else {
      Some(headers)
    }
    val optionalBody = if(body == null || body.isEmpty) {
      None
    } else {
      Some(body)
    }
    Response(status, optionalHeaders, optionalBody)
  }

  val invalidRequest = Response(500, Map[String, String](), s"""{"error": "unexpected request"}""")
}


