package au.com.dius.pact.consumer.specs2

import java.util.concurrent.TimeUnit.MILLISECONDS

import au.com.dius.pact.consumer.PactSpec
import au.com.dius.pact.model.PactFragment
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

  override def is = PactFragment.consumer(consumer).hasPactWith(provider)
    .given("foo_state")
    .uponReceiving("a request for foo")
      .matching(path = "/foo")
      .willRespondWith(body = "{}")
    .given("bar_state")
    .uponReceiving("an option request for bar")
      .matching(path = "/", method = "OPTION")
      .willRespondWith(headers = Map("Option" -> "Value-X"))
    .given()
    .uponReceiving("a stateless request for foobar")
      .matching(path = "/foobar")
      .willRespondWith(body = "{}")
    .withConsumerTest(providerConfig => {
      val optionsResult = ConsumerService(providerConfig.url).options("/")
      val simpleGet = ConsumerService(providerConfig.url).simpleGet("/foo")
      val simpleStatelessGet = ConsumerService(providerConfig.url).simpleGet("/foobar")
      Await.result(optionsResult, timeout) must be_==(200, "",
        Map("Content-Length" -> "0", "Connection" -> "keep-alive", "Option" -> "Value-X")) and
        (Await.result(simpleGet, timeout) must be_==(200, "{}")) and
        (Await.result(simpleStatelessGet, timeout) must be_==(200, "{}"))
    })

}
