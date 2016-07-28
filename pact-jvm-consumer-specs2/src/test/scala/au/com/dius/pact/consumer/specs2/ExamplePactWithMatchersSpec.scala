package au.com.dius.pact.consumer.specs2

import java.util.concurrent.TimeUnit.MILLISECONDS

import au.com.dius.pact.consumer.PactSpec
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import org.json.JSONObject
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class ExamplePactWithMatchersSpec extends Specification with PactSpec {

  val consumer = "My Consumer"
  val provider = "My Provider"

  val timeout = Duration(5000, MILLISECONDS)

  val body = new PactDslJsonBody()
    .stringMatcher("foo", "\\d+")
    .stringMatcher("bar", "[aA]+")

  override def is = uponReceiving("a request for foo with a body")
      .matching(path = "/foo")
      .willRespondWith(
        status = 200,
        headers = Map.empty[String, String],
        bodyAndMatchers = body
      )
    .withConsumerTest(providerConfig => {
      val (status, body) = Await.result(ConsumerService(providerConfig.url).simpleGet("/foo"), timeout)
      val bodyJson = new JSONObject(body)

      (status ==== 200) and
        (bodyJson.getInt("foo") must be > 0) and
        ((bodyJson.getString("bar").length > 0) ==== true)
    })
}
