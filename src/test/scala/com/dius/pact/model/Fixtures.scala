package com.dius.pact.model

object Fixtures {


  val consumer = Consumer("consumer")
  val provider = Provider("provider")

  val jsonHeaders = Map[String, String]("Content-Type" -> "application/json")
  val requestHeaders = Map[String, String]("Content-Type" -> "application/json")
  val requestBody = """{"state" : "alive and well"}"""

  val request = Request("post", "/request/path.json", Some(requestHeaders), Some(requestBody))
  val response = Response(200, jsonHeaders, "responseBody")

  val secondResponse = Response(404, jsonHeaders, "not found response")

  val interaction = Interaction(
  "description",
  "state",
  request,
  response)

  val pact = {
    Pact(provider, consumer, Seq(interaction))
  }
}
