package com.dius.pact.model

import org.json4s._

case class MakeInteraction(providerState: String,
                           description: Option[String] = None,
                           request: Option[Request] = None,
                           response: Option[Response] = None) {
  import HttpMethod._
  def uponReceiving(description: String,
                    path: String,
                    method: String = Get,
                    headers: Option[Map[String, String]] = None,
                    body: Option[JValue] = None):MakeInteraction = {
    val r = Request(method, path, headers, body)
    copy(description = Some(description), request = Some(r))
  }

  def willRespondWith(status:Int = 200,
                      headers: Option[Map[String,String]] = None,
                      body: Option[JValue] = None) = {
    copy(response = Some(Response(status, headers, body)))
  }
}

object MakeInteraction {
  implicit def someify[T](t:T):Option[T] = Some(t)

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
  def build = MakePact.build(this)
}

object MakePact {
  implicit def build(mp:MakePact):Pact = {
    Pact(mp.provider.get, mp.consumer.get, mp.interactions)
  }
}
