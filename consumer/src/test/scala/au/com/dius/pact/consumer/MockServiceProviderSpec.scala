package au.com.dius.pact.consumer

import org.specs2.mutable.Specification
import Fixtures._
import akka.actor.ActorSystem
import scala.concurrent.Future
import akka.io.IO
import akka.pattern.ask
import spray.can.Http
import spray.http._
import au.com.dius.pact.model._
import au.com.dius.pact.model.spray.Conversions._
import scala.concurrent.duration.FiniteDuration
import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JObject}
import au.com.dius.pact.consumer.{PactServerConfig, MockServiceProvider}

class MockServiceProviderSpec extends Specification {
  
  implicit val timeout = FiniteDuration(10L, "second")
  
  def http(r:HttpRequest)(implicit system: ActorSystem): Future[HttpResponse] = {
    implicit val executionContext = system.dispatcher
    ask(IO(Http), r)(5000L).mapTo[HttpResponse]
  }

  //TODO: move PactServer startup and shutdown into an around function
  "Pact Server" should {
    "Work" in {
      implicit val system = ActorSystem()
      implicit val executionContext = system.dispatcher

      val config = PactServerConfig(port = 9999)

      val server = MockServiceProvider(config, pact)
      //this line ensures the server is running before we start hitting it
      server.start must beEqualTo(server).await(timeout = timeout)

      server.enterState(interaction.providerState) must beEqualTo(server).await(timeout = timeout)

      val host = s"${config.interface}:${config.port}"
      //hit server with invalid request, add spray headers for easier verification
      val invalidRequest = request.copy(path = s"${config.url}/foo")
      val inValidResponse: Future[HttpResponse] = http(invalidRequest)
      inValidResponse.map{_.status.intValue} must beEqualTo(500).await(timeout = timeout)

      //hit server with valid request
      val validResponse: Future[HttpResponse] = http(request.copy(path = s"${config.url}/"))
      validResponse.map{_.status.intValue} must beEqualTo(response.status).await(timeout = timeout)

      val expectedInteractions = List(
        Interaction("MockServiceProvider received",
          "test state",
          Request("GET", "/foo",
            Map(
              "user-agent" -> "spray-can/1.2-RC1",
              "host" -> host,
              "content-length" -> "13",
              "testreqheader" -> "testreqheadervalue",
              "content-type" -> "application/json; charset=UTF-8"),
            "test" -> true),
          Response(500, None,
            Some(JObject("error" -> JString("unexpected request")))
        )),
        Interaction("MockServiceProvider received",
          "test state",
          Request("GET", "/",
            Map(
              "user-agent" -> "spray-can/1.2-RC1",
              "host" -> host,
              "content-length" -> "13",
              "testreqheader" -> "testreqheadervalue",
              "content-type" -> "application/json; charset=UTF-8"),
            "test" -> true),
          Response(200, Map("testreqheader" -> "testreqheaderval"),
            "responsetest" -> true))
      )

      //TODO: fix this up
//      server.interactions must beEqualTo(expectedInteractions).await(timeout = timeout)

      server.stop must beEqualTo(server).await(timeout = timeout)

      system.shutdown()

      true must beTrue
    }
  }
}