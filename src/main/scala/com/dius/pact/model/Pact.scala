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

case class Request(path:String)

case class Response(body:String)
