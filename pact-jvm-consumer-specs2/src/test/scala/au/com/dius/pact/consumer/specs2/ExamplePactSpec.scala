package au.com.dius.pact.consumer.specs2

import au.com.dius.pact.consumer.PactSpec
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ExamplePactSpec extends PactSpec {
  
  val consumer = "My Consumer"
  val provider = "My Provider"

  uponReceiving("a request for foo")
    .matching(path = "/foo")
    .willRespondWith(body = "{}")
  .during { providerConfig =>
    ConsumerService(providerConfig.url).simpleGet("/foo") must beEqualTo(200, Some("{}")).await
  }
}
