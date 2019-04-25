package au.com.dius.pact.consumer.specs2

import java.util.concurrent.TimeUnit.MILLISECONDS

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class ExamplePactSpec extends Specification with PactSpec {

  val consumer = "My Consumer"
  val provider = "My Provider"
  override val providerState = Some("foo_state")

  val timeout = Duration(5000, MILLISECONDS)

  override def is = uponReceiving("a request for foo")
      .matching(path = "/foo")
      .willRespondWith(maybeBody = Some("{}"))
    .uponReceiving("an option request")
      .matching(path = "/", method = "OPTION")
      .willRespondWith(headers = Map("Option" -> List("Value-X")))
    .withConsumerTest((mockServer, _) => {
      val optionsResult = ConsumerService(mockServer.getUrl).options("/")
      val simpleGet = ConsumerService(mockServer.getUrl).simpleGet("/foo")
      Await.result(optionsResult, timeout) must be_==(200, "",
        Map("Content-Length" -> "0", "Connection" -> "keep-alive", "Option" -> "Value-X")) and
        (Await.result(simpleGet, timeout) must be_==(200, "{}"))
    })

}
