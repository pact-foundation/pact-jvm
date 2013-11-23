package com.dius.pact.model

import play.api.libs.json.{Json, JsValue}

trait HttpMethod

case object Get extends HttpMethod {
  override def toString = "GET"
}

//TODO: use a chain of 3 single interface objects rather than allow for exception on get further down
case class MakeInteraction(providerState: String,
                           description: Option[String] = None,
                           request: Option[Request] = None,
                           response: Option[Response] = None) {

  def uponReceiving(description: String,
                    path: String,
                    method: HttpMethod = Get,
                    headers: Option[Map[String, String]] = None,
                    body: Option[JsValue] = None) = {
    val r = Request(method.toString, path, headers, body.map(Json.stringify))
    copy(description = Some(description), request = Some(r))
  }

  def willRespondWith(status:Int = 200,
                      headers: Option[Map[String,String]] = None,
                      body: Option[JsValue] = None) = {
    copy(response = Some(Response(status, headers, body.map(Json.stringify))))
  }
}

object MakeInteraction {
  def given(state:String) = MakeInteraction(providerState = state)

  implicit def build(mi: MakeInteraction):Interaction = {
    Interaction(mi.description.get,
      mi.providerState,
      mi.request.get,
      mi.response.get)
  }

  implicit def build(mis: Seq[MakeInteraction]) : Seq[Interaction] = {
    mis.map(mi => build(mi))
  }
}

case class MakePact(
                     provider:Option[Provider] = None,
                     consumer:Option[Consumer] = None,
                     interactions:Seq[MakeInteraction] = Seq()) {
  def withProvider(name:String) = copy(provider = Some(Provider(name)))
  def withConsumer(name:String) = copy(consumer = Some(Consumer(name)))
  def withInteractions(list:MakeInteraction*) = copy(interactions = list)
}

object MakePact {
  implicit def build(mp:MakePact):Pact = {
    Pact(mp.provider.get, mp.consumer.get, mp.interactions)
  }
}
