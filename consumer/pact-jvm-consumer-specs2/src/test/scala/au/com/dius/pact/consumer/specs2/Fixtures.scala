package au.com.dius.pact.consumer.specs2

import java.util

import au.com.dius.pact.core.model.{RequestResponseInteraction, _}

import scala.collection.JavaConverters._

object Fixtures {

  val provider = new Provider("test_provider")
  val consumer = new Consumer("test_consumer")

  val request = new Request("POST", "/", PactReaderKt.queryStringToMap("q=p"),
    Map("testreqheader" -> List("testreqheadervalue").asJava).asJava,
    OptionalBody.body("{\"test\": true}".getBytes))

  val response = new Response(200,
    Map("testreqheader" -> List("testreqheaderval").asJava, "Access-Control-Allow-Origin" -> List("*").asJava).asJava,
    OptionalBody.body("{\"responsetest\": true}".getBytes))

  val interaction = new RequestResponseInteraction("test interaction",
    Seq(new ProviderState("test state")).asJava, request, response)

  val pact: RequestResponsePact = new RequestResponsePact(provider, consumer, util.Arrays.asList(interaction))
}
