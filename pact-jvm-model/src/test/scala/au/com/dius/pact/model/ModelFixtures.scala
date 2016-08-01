package au.com.dius.pact.model

import java.util

import scala.collection.JavaConversions

object ModelFixtures {

  private def scalaMMapToJavaMMap(map: Map[String, Map[String, AnyRef]]) : java.util.Map[String, java.util.Map[String, AnyRef]] = {
    JavaConversions.mapAsJavaMap(map.mapValues {
      case jmap: Map[String, _] => JavaConversions.mapAsJavaMap(jmap)
    })
  }

  private def scalaLMaptoJavaLMap(map: Map[String, List[String]]): util.Map[String, util.List[String]] = {
    JavaConversions.mapAsJavaMap(map.mapValues {
      case jlist: List[String] => JavaConversions.seqAsJavaList(jlist.toSeq)
    })
  }

  val request = new Request("GET", "/", PactReader.queryStringToMap("q=p&q=p2&r=s"),
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheadervalue")),
     OptionalBody.body("{\"test\":true}"))

  val response = new Response(200,
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheaderval")),
    OptionalBody.body("{\"responsetest\":true}"))

  val requestWithMatchers = new Request("GET", "/", PactReader.queryStringToMap("q=p&q=p2&r=s"),
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheadervalue")),
    OptionalBody.body("{\"test\":true}"), scalaMMapToJavaMMap(Map("$.body.test" -> Map("match" -> "type"))))

  val responseWithMatchers = new Response(200,
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheaderval")),
    OptionalBody.body("{\"responsetest\":true}"),
    scalaMMapToJavaMMap(Map("$.body.responsetest" -> Map("match" -> "type"))))

  val requestNoBody = new Request("GET", "/", PactReader.queryStringToMap("q=p&q=p2&r=s"),
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheadervalue")))

  val requestDecodedQuery = new Request("GET", "/",
    scalaLMaptoJavaLMap(Map("datetime" -> List("2011-12-03T10:15:30+01:00"),
      "description" -> List("hello world!"))),
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheadervalue")),
    OptionalBody.body("{\"test\":true}"))

  val responseNoBody = new Response(200, JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheaderval")))

  val requestLowerCaseMethod = new Request("get", "/", PactReader.queryStringToMap("q=p&q=p2&r=s"),
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheadervalue")),
    OptionalBody.body("{\"test\":true}"))

  val interaction = new RequestResponseInteraction("test interaction", "test state", request, response)
}
