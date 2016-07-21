package au.com.dius.pact.model

import java.util

import scala.collection.JavaConversions
import scala.collection.JavaConverters._

object ModelFixtures {

  val provider = new Provider("test_provider")
  val consumer = new Consumer("test_consumer")

  val request = new Request(HttpMethod.Get, "/", PactReader.queryStringToMap("q=p&q=p2&r=s"),
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheadervalue")),
     OptionalBody.body("{\"test\":true}"))

  val response = new Response(200,
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheaderval")),
    OptionalBody.body("{\"responsetest\":true}"))

  val requestNoBody = new Request(HttpMethod.Get, "/", PactReader.queryStringToMap("q=p&q=p2&r=s"),
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheadervalue")))

  val requestDecodedQuery = new Request(HttpMethod.Get, "/",
    CollectionUtils.scalaLMaptoJavaLMap(Map("datetime" -> List("2011-12-03T10:15:30+01:00"),
      "description" -> List("hello world!"))),
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheadervalue")),
    OptionalBody.body("{\"test\":true}"))

  val responseNoBody = new Response(200, JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheaderval")))

  val requestLowerCaseMethod = new Request("get", "/", PactReader.queryStringToMap("q=p&q=p2&r=s"),
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheadervalue")),
    OptionalBody.body("{\"test\":true}"))

  val providerStates = Seq(new ProviderState("test state")).asJava

  val interaction = new RequestResponseInteraction("test interaction", providerStates, request, response)

  val interactionsWithNoBodies = List(new RequestResponseInteraction("test interaction with no bodies", providerStates,
    requestNoBody, responseNoBody))

  val interactionsWithDecodedQuery = List(new RequestResponseInteraction("test interaction", providerStates,
    requestDecodedQuery, response))

  val interactionsWithLowerCaseMethods = List(new RequestResponseInteraction("test interaction", providerStates,
    requestLowerCaseMethod, response))

  val interactions = List(interaction)

  val pact: RequestResponsePact = new RequestResponsePact(provider, consumer, JavaConversions.seqAsJavaList(interactions.toSeq))

  val pactWithNoBodies: RequestResponsePact = new RequestResponsePact(provider, consumer, JavaConversions.seqAsJavaList(interactionsWithNoBodies.toSeq))

  val pactDecodedQuery = new RequestResponsePact(provider, consumer, JavaConversions.seqAsJavaList(interactionsWithDecodedQuery.toSeq))

  val pactWithLowercaseMethods: RequestResponsePact = new RequestResponsePact(provider, consumer, JavaConversions.seqAsJavaList(interactionsWithLowerCaseMethods.toSeq))

}
