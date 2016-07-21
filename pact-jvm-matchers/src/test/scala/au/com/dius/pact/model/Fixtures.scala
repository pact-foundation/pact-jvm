package au.com.dius.pact.model

import java.util

import scala.collection.JavaConversions
import scala.collection.JavaConverters._

object Fixtures {
  import HttpMethod._

  val provider = new Provider("test_provider")
  val consumer = new Consumer("test_consumer")


  val request = new Request(Get, "/", PactReader.queryStringToMap("q=p&q=p2&r=s"),
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheadervalue")),
    OptionalBody.body("{\"test\": true}"))

  val response = new Response(200, JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheaderval")),
    OptionalBody.body("{\"responsetest\": true}"))

  val interaction = new RequestResponseInteraction("test interaction", Seq(new ProviderState("test state")).asJava,
    request, response)

  val interactions = util.Arrays.asList(interaction)

  val pact: RequestResponsePact = new RequestResponsePact(provider, consumer, interactions)
}
