package com.dius.pact.model

object Fixtures {
  val request = Request("requestPath")
  val response = Response("responseBody")
  val secondResponse = Response("second response")

  val interaction = Interaction(
    "description",
    "state",
    request,
    response)

  val pact = Pact(Provider("provider"), Consumer("consumer"), Seq(interaction))
}
