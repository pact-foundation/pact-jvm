package au.com.dius.pact.model

import scala.collection.JavaConversions

object ModelFixtures {

  val provider = new Provider("test_provider")
  val consumer = new Consumer("test_consumer")

  val request = new Request(HttpMethod.Get, "/", PactReader.queryStringToMap("q=p&q=p2&r=s"),
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheadervalue")),
     OptionalBody.body("{\"test\":true}"))

  val response = new Response(200,
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheaderval")),
    OptionalBody.body("{\"responsetest\":true}"))

  val requestWithMatchers = new Request(HttpMethod.Get, "/", PactReader.queryStringToMap("q=p&q=p2&r=s"),
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheadervalue")),
    OptionalBody.body("{\"test\":true}"), CollectionUtils.scalaMMapToJavaMMap(Map("$.body.test" -> Map("match" -> "type"))))

  val responseWithMatchers = new Response(200,
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheaderval")),
    OptionalBody.body("{\"responsetest\":true}"),
    CollectionUtils.scalaMMapToJavaMMap(Map("$.body.responsetest" -> Map("match" -> "type"))))

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

  val interaction = new RequestResponseInteraction("test interaction", "test state", request, response)

  val interactionsWithMatchers = List(new RequestResponseInteraction("test interaction with matchers", "test state",
    requestWithMatchers, responseWithMatchers))

  val interactionsWithNoBodies = List(new RequestResponseInteraction("test interaction with no bodies", "test state",
    requestNoBody, responseNoBody))

  val interactionsWithDecodedQuery = List(new RequestResponseInteraction("test interaction", "test state",
    requestDecodedQuery, response))

  val interactionsWithLowerCaseMethods = List(new RequestResponseInteraction("test interaction", "test state",
    requestLowerCaseMethod, response))

  val interactions = List(interaction)

  val pact: RequestResponsePact = new RequestResponsePact(provider, consumer, JavaConversions.seqAsJavaList(interactions.toSeq))

  val pactWithMatchers: RequestResponsePact = new RequestResponsePact(provider, consumer, JavaConversions.seqAsJavaList(interactionsWithMatchers.toSeq))

  val pactWithNoBodies: RequestResponsePact = new RequestResponsePact(provider, consumer, JavaConversions.seqAsJavaList(interactionsWithNoBodies.toSeq))

  val pactDecodedQuery = new RequestResponsePact(provider, consumer, JavaConversions.seqAsJavaList(interactionsWithDecodedQuery.toSeq))

  val pactWithLowercaseMethods: RequestResponsePact = new RequestResponsePact(provider, consumer, JavaConversions.seqAsJavaList(interactionsWithLowerCaseMethods.toSeq))

}
