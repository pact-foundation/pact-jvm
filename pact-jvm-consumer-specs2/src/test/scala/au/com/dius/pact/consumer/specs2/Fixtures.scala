package au.com.dius.pact.consumer.specs2

import java.util

import au.com.dius.pact.model.{Consumer, Interaction, Provider, _}

object Fixtures {
  import au.com.dius.pact.model.HttpMethod._
  import scala.collection.JavaConversions._

  val provider = new Provider("test_provider")
  val consumer = new Consumer("test_consumer")

  val request = new Request(Post, "/", PactReader.queryStringToMap("q=p"),
    Map("testreqheader" -> "testreqheadervalue").asInstanceOf[java.util.Map[String, String]],
    "{\"test\": true}")

  val response = new Response(200,
    Map("testreqheader" -> "testreqheaderval", "Access-Control-Allow-Origin" -> "*").asInstanceOf[java.util.Map[String, String]],
    "{\"responsetest\": true}")

  val interaction = new Interaction("test interaction", "test state", request, response)

  val pact: Pact = new Pact(provider, consumer, util.Arrays.asList(interaction))
}
