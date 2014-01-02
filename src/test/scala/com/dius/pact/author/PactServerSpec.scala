package com.dius.pact.author

import org.specs2.mutable.Specification
import Fixtures._
import akka.actor.ActorSystem
import scala.concurrent.Future
import akka.io.IO
import akka.pattern.ask
import spray.can.Http
import spray.http._
import com.dius.pact.model._
import com.dius.pact.model.HttpMethod.build
import com.dius.pact.model.spray.Conversions._
import scala.concurrent.duration.FiniteDuration

class PactServerSpec extends Specification {
  
  implicit val timeout = FiniteDuration(10L, "second")
  
  def http(r:HttpRequest)(implicit system: ActorSystem): Future[HttpResponse] = {
    implicit val executionContext = system.dispatcher
    val f = ask(IO(Http), r)(5000L)
    f.onFailure { case t:Throwable => {
      t.printStackTrace()
    }}
    f.mapTo[HttpResponse]
  }

  //TODO: move PactServer startup and shutdown into an around function
  "Pact Server" should {
    "Work" in {
      implicit val system = ActorSystem()
      implicit val executionContext = system.dispatcher

      val server = PactServer(pact)
      //this line ensures the server is running before we start hitting it
      server.start must beEqualTo(server).await(timeout = timeout)

      server.enterState(interaction.providerState) must beEqualTo(server).await(timeout = timeout)

      //hit server with invalid request
      val inValidResponse: Future[HttpResponse] = http(request.copy(path = s"http://${Config.interface}:${Config.port}/foo"))
      inValidResponse.map{_.status.intValue} must beEqualTo(500).await(timeout = timeout)

      //hit server with valid request
      val validResponse: Future[HttpResponse] = http(request.copy(path = s"http://${Config.interface}:${Config.port}/"))
      validResponse.map{_.status.intValue} must beEqualTo(response.status).await(timeout = timeout)

      val expectedInteractions = List(
        Interaction("",
          "test state",
          Request(build("GET"), "/foo",
            Some(Map(
              "user-agent" -> "spray-can/1.2-RC1",
              "host" -> "localhost:9090",
              "content-length" -> "13",
              "testreqheader" -> "testreqheadervalue",
              "content-type" -> "application/json; charset=UTF-8")),
            Some("""{"test":true}""")),
          Response(500, None,
            Some("""{"error": "unexpected request Request(GET,/foo,Some(Map(user-agent -> spray-can/1.2-RC1, host -> localhost:9090, content-length -> 13, testreqheader -> testreqheadervalue, content-type -> application/json; charset=UTF-8)),Some({"test":true}))"}"""))
        ),
        Interaction("",
          "test state",
          Request(build("GET"), "/",
            Some(Map(
              "user-agent" -> "spray-can/1.2-RC1",
              "host" -> "localhost:9090",
              "content-length" -> "13",
              "testreqheader" -> "testreqheadervalue",
              "content-type" -> "application/json; charset=UTF-8")),
            Some("""{"test":true}""")),
          Response(200, Some(Map("testreqheader" -> "testreqheaderval")),
            Some("""{"responsetest":true}"""))))


      server.interactions.map(_.toString) must beEqualTo(expectedInteractions.toString).await(timeout = timeout)

      server.stop must beEqualTo(server).await(timeout = timeout)
    }
  }
}
