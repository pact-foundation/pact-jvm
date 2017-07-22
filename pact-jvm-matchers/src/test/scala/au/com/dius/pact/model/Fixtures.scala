package au.com.dius.pact.model

import java.util

import scala.collection.JavaConversions

object Fixtures {

  val provider = new Provider("test_provider")
  val consumer = new Consumer("test_consumer")


  val request = new Request("GET", "/", PactReader.queryStringToMap("q=p&q=p2&r=s"),
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheadervalue")),
    OptionalBody.body("{\"test\": true}"))

  val response = new Response(200, JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheaderval")),
    OptionalBody.body("{\"responsetest\": true}"))

  val interaction = new RequestResponseInteraction("test interaction", "test state", request, response)

  val interactions = util.Arrays.asList(interaction)

  val pact: RequestResponsePact = new RequestResponsePact(provider, consumer, interactions)

  val headerMatcher = Map("$.headers.HEADER" -> Map("regex" -> ".*"))
  val contentTypeHeaderMatcher = Map("$.headers.CONTENT-TYPE" -> Map("regex" -> "[a-z]+\\/[a-z]+"))
}
