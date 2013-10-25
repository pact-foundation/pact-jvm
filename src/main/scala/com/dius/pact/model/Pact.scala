package com.dius.pact.model

case class Pact(
                 provider:Provider,
                 consumer:Consumer,
                 interactions:Seq[Interaction]) {
  def interactionFor(providerState:String, description:String) = interactions.find { i =>
    i.description == description && i.providerState == providerState
  }
}

case class Provider(name:String)

case class Consumer(name:String)

case class Interaction(
                        description:   String,
                        providerState: String,
                        request:       Request,
                        response:      Response)

case class Request(path:String)

case class Response(body:String)


case class MakeInteraction(
                            description:   Option[String] = None,
                            providerState: Option[String] = None) {
  def uponReceiving(desc:String) = copy(description = Some(desc))
}

object MakeInteraction {

  def given(state:String) = MakeInteraction(providerState = Some(state))

  implicit def build(mi: MakeInteraction):Interaction = {
    Interaction(mi.description.get, mi.providerState.get, null, null) //FIXME: lol null
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
