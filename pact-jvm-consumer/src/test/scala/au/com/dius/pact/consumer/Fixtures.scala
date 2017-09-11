package au.com.dius.pact.consumer

import java.util

import au.com.dius.pact.model._

import scala.collection.JavaConversions
import scala.collection.JavaConverters._

object Fixtures {

  val provider = new Provider("test_provider")
  val consumer = new Consumer("test_consumer")

  val headers = Map("testreqheader" -> "testreqheadervalue", "Content-Type" -> "application/json")
  val request = new Request("POST", "/", null, JavaConversions.mapAsJavaMap(headers), OptionalBody.body("{\"test\": true}"))

  val response = new Response(200,
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheaderval", "Access-Control-Allow-Origin" -> "*")),
    OptionalBody.body("{\"responsetest\": true}"))

  val interaction = new RequestResponseInteraction("test interaction", Seq(new ProviderState("test state")).asJava,
    request, response)

  val pact: RequestResponsePact = new RequestResponsePact(provider, consumer, util.Arrays.asList(interaction))
}
