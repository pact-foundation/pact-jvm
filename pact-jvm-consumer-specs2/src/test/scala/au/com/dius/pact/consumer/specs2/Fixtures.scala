package au.com.dius.pact.consumer.specs2

import java.util

import au.com.dius.pact.model.{Consumer, Provider, RequestResponseInteraction, _}

object Fixtures {
  import au.com.dius.pact.model.HttpMethod._

  val provider = new Provider("test_provider")
  val consumer = new Consumer("test_consumer")

  val request = new Request(Post, "/", PactReader.queryStringToMap("q=p"),
    Map("testreqheader" -> "testreqheadervalue").asInstanceOf[java.util.Map[String, String]],
    OptionalBody.body("{\"test\": true}"))

  val response = new Response(200,
    Map("testreqheader" -> "testreqheaderval", "Access-Control-Allow-Origin" -> "*").asInstanceOf[java.util.Map[String, String]],
    OptionalBody.body("{\"responsetest\": true}"))

  val interaction = new RequestResponseInteraction("test interaction", "test state", request, response)

  val pact: RequestResponsePact = new RequestResponsePact(provider, consumer, util.Arrays.asList(interaction))
}
