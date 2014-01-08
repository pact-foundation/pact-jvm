package com.dius.pact.model.spray

import org.specs2.mutable.Specification
import com.dius.pact.model.{Request, Response}
import spray.http._
import spray.http.HttpHeaders.RawHeader
import org.json4s.JsonDSL._


//TODO: convert this to a quickcheck/scalacheck style test to get great combination coverage
class ConversionsSpec extends Specification {

  import Conversions._
  import com.dius.pact.model.HttpMethod._

  def convert[T, Z](from:T, to:Z)(implicit f:(T) => Z) = {
    val converted:Z = from
    converted must beEqualTo(to)
  }

  "Request" should {
    val emptyPactRequest = Request(Get, "/", None, None)

    val emptySprayRequest = HttpRequest()

    val fullPactRequest = Request(Post, "/post",
      headers = Some(Map("foo" -> "bar")),
      body = Some("json" -> "values")
    )

    val fullSprayRequest = HttpRequest(
      HttpMethods.POST,
      Uri("/post"),
      List(RawHeader("foo", "bar")),
      HttpEntity(ContentTypes.`application/json`, """{"json":"values"}"""))


    "Spray to Pact" in {
      convert(emptySprayRequest, emptyPactRequest)
      convert(fullSprayRequest, fullPactRequest)
    }

    "Pact to Spray" in {
      convert(emptyPactRequest, emptySprayRequest)
      convert(fullPactRequest, fullSprayRequest)
    }
  }

  "Response" should {
    val emptyPactResponse = Response(200, None, None)

    val emptySprayResponse = HttpResponse()

    val fullPactResponse = Response(201, Map("foo" -> "bar"), "json" -> true)

    val fullSprayResponse = HttpResponse(
      StatusCodes.Created,
      HttpEntity(ContentTypes.`application/json`, """{"json":true}"""),
      List(RawHeader("foo", "bar")))


    "Spray to Pact" in {
      convert(emptySprayResponse, emptyPactResponse)
      convert(fullSprayResponse, fullPactResponse)
    }

    "Pact to Spray" in {
      convert(emptyPactResponse, emptySprayResponse)
      convert(fullPactResponse, fullSprayResponse)
    }
  }

}
