package au.com.dius.pact.consumer.specs2

import java.util.concurrent.TimeUnit.MILLISECONDS

import au.com.dius.pact.core.model.Consumer
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class DifferentStatesPactSpec extends Specification with PactSpec {

  val consumer = "My Consumer"
  val provider = "My Provider"

  val timeout = Duration(5000, MILLISECONDS)

  override def is = PactFragmentBuilder(new Consumer(consumer)).hasPactWith(provider)
    .given("foo_state")
    .given("bar state", Map("ValueA" -> "A"))
    .uponReceiving("a request for foo")
      .matching(path = "/foo")
      .willRespondWith(maybeBody = Some("{}"))
    .given("bar_state", Map("ValueA" -> "B"))
    .uponReceiving("an option request for bar")
      .matching(path = "/", method = "OPTION")
      .willRespondWith(headers = Map("Option" -> List("Value-X")))
    .given()
    .uponReceiving("a stateless request for foobar")
      .matching(path = "/foobar")
      .willRespondWith(maybeBody = Some("{}"))
    .withConsumerTest((mockServer, _) => {
      val optionsResult = ConsumerService(mockServer.getUrl).options("/")
      val simpleGet = ConsumerService(mockServer.getUrl).simpleGet("/foo")
      val simpleStatelessGet = ConsumerService(mockServer.getUrl).simpleGet("/foobar")
      Await.result(optionsResult, timeout) must be_==(200, "",
        Map("Content-Length" -> "0", "Connection" -> "keep-alive", "Option" -> "Value-X")) and
        (Await.result(simpleGet, timeout) must be_==(200, "{}")) and
        (Await.result(simpleStatelessGet, timeout) must be_==(200, "{}"))
    })

}
