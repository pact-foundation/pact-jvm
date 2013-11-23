package com.dius.pact.model

case class Pact(provider:Provider, consumer:Consumer, interactions:Seq[Interaction]) {
  def interactionFor(description:String, providerState:String) = interactions.find { i =>
    i.description == description && i.providerState == providerState
  }
}

case class Provider(name:String)

case class Consumer(name:String)

case class Interaction(
                        description: String,
                        providerState: String,
                        request:Request,
                        response:Response)

//TODO: support duplicate headers
case class Request(method: String,
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
}


