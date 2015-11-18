package au.com.dius.pact.model

import org.json4s.JsonAST.{JBool, JObject}
import org.json4s.jackson.JsonMethods.compact

object Fixtures {

  import scala.collection.JavaConversions._

  val provider = new Provider("test_provider")
  val consumer = new Consumer("test_consumer")

  val request = new Request(HttpMethod.Get, "/", PactReader.queryStringToMap("q=p&q=p2&r=s"),
    Map("testreqheader" -> "testreqheadervalue").asInstanceOf[java.util.Map[String, String]],
    compact(JObject("test" -> JBool(true))))

  val response = new Response(200,
    Map("testreqheader" -> "testreqheaderval").asInstanceOf[java.util.Map],
    compact(JObject("responsetest" -> JBool(true))))

  val requestWithMatchers = new Request(HttpMethod.Get, "/", PactReader.queryStringToMap("q=p&q=p2&r=s"),
    Map("testreqheader" -> "testreqheadervalue").asInstanceOf[java.util.Map],
    compact(JObject("test" -> JBool(true))), Map("$.body.test" -> Map("match" -> "type")).asInstanceOf[java.util.Map])

  val responseWithMatchers = new Response(200,
    Map("testreqheader" -> "testreqheaderval").asInstanceOf[java.util.Map],
    compact(JObject("responsetest" -> JBool(true))),
    Map("$.body.responsetest" -> Map("match" -> "type")).asInstanceOf[java.util.Map])

  val requestNoBody = new Request(HttpMethod.Get, "/", PactReader.queryStringToMap("q=p&q=p2&r=s"),
    Map("testreqheader" -> "testreqheadervalue").asInstanceOf[java.util.Map])

  val requestDecodedQuery = new Request(HttpMethod.Get, "/",
    Map("datetime" -> List("2011-12-03T10:15:30+01:00").asInstanceOf[java.util.List],
      "description" -> List("hello world!").asInstanceOf[java.util.List]).asInstanceOf[java.util.Map],
    Map("testreqheader" -> "testreqheadervalue").asInstanceOf[java.util.Map],
    compact(JObject("responsetest" -> JBool(true))))

  val responseNoBody = new Response(200, Map("testreqheader" -> "testreqheaderval").asInstanceOf[java.util.Map])

  val requestLowerCaseMethod = new Request("get", "/", "q=p&q=p2&r=s",
    Map("testreqheader" -> "testreqheadervalue").asInstanceOf[java.util.Map[String, String]],
    compact(JObject("test" -> JBool(true))))

  val interaction = new Interaction("test interaction", "test state", request, response)

  val interactionsWithMatchers = List(new Interaction("test interaction with matchers", "test state",
    requestWithMatchers, responseWithMatchers))

  val interactionsWithNoBodies = List(new Interaction("test interaction with no bodies", "test state",
    requestNoBody, responseNoBody))

  val interactionsWithDecodedQuery = List(new Interaction("test interaction with decoded query", "test state",
    requestDecodedQuery, response))

  val interactionsWithLowerCaseMethods = List(new Interaction("test interaction with lower case method", "test state",
    requestLowerCaseMethod, response))

  val interactions = List(interaction)

  val pact: Pact = new Pact(provider, consumer, interactions.asInstanceOf[java.util.List[Interaction]])

  val pactWithMatchers: Pact = new Pact(provider, consumer, interactionsWithMatchers.asInstanceOf[java.util.List[Interaction]])

  val pactWithNoBodies: Pact = new Pact(provider, consumer, interactionsWithNoBodies.asInstanceOf[java.util.List[Interaction]])

  val pactDecodedQuery = new Pact(provider, consumer, interactionsWithDecodedQuery.asInstanceOf[java.util.List[Interaction]])

  val pactWithLowercaseMethods: Pact = new Pact(provider, consumer, interactionsWithLowerCaseMethods.asInstanceOf[java.util.List[Interaction]])

}
