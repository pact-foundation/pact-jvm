package com.dius.pact.runner

import _root_.spray.http._
import _root_.spray.http.HttpHeaders.RawHeader
import _root_.spray.http.HttpRequest
import _root_.spray.http.HttpResponse
import scala.concurrent.{Future, Await}
import org.scalatest.{Assertions, FreeSpec}
import scala.concurrent.duration.Duration
import com.dius.pact.model._
import com.dius.pact.model.Matching._
import akka.actor.ActorSystem
import org.json4s.jackson.JsonMethods._
import com.dius.pact.model.spray.Conversions


class PactSpec(config: PactConfiguration, pact: Pact)(implicit actorSystem: ActorSystem) extends FreeSpec with Assertions {
  implicit val executionContext = actorSystem.dispatcher
  val pipeline: HttpRequest => Future[HttpResponse] = _root_.spray.client.pipelining.sendReceive

  object EnterStateRequest {
    def apply(url: String, state: String): HttpRequest = {
      import org.json4s.JsonDSL._
      val jsonString = compact(render("state" -> state))
      println(s"entering state $state")
      HttpRequest(method = HttpMethods.POST, uri = url, entity = HttpEntity(ContentTypes.`application/json`, jsonString))
    }
  }

  object ServiceInvokeRequest {
    def apply(url: String, request: Request):HttpRequest = {
      val method = HttpMethods.getForKey(request.method.toString.toUpperCase).get
      val uri = Uri(s"$url${request.path}")
      val headers: List[HttpHeader] = request.headers.map(_.toList.map{case (key, value) => RawHeader(key, value)}).getOrElse(Nil)
      val entity: HttpEntity = request.bodyString.map(HttpEntity(_)).getOrElse(HttpEntity.Empty)
      println(s"invoking service with: $request")
      HttpRequest(method, uri, headers, entity)
    }
  }

  pact.interactions.toList.map { interaction =>
    s"""pact for consumer ${pact.consumer.name} """ +
      s"""provider ${pact.provider.name} """ +
      s"""interaction "${interaction.description}" """ +
      s"""in state: "${interaction.providerState}" """ in {
      val response = for {
        inState <- pipeline(EnterStateRequest(config.stateChangeUrl, interaction.providerState))
        sprayResponse <- pipeline(ServiceInvokeRequest(config.providerBaseUrl, interaction.request))
        pactResponse = Conversions.sprayToPactResponse(sprayResponse)
      } yield pactResponse

      val actualResponse = Await.result(response, Duration(config.timeoutSeconds, "s"))

      assert(ResponseMatching.matchRules(interaction.response, actualResponse) === MatchFound)
    }
  }
}