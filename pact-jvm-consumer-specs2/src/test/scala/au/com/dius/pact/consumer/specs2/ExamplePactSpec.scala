package au.com.dius.pact.consumer.specs2

import java.util.concurrent.TimeUnit.MILLISECONDS

import au.com.dius.pact.consumer.PactSpec
import org.junit.runner.RunWith
import org.specs2.concurrent.ExecutionEnv
import org.specs2.runner.JUnitRunner

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class ExamplePactSpec extends PactSpec {

  val consumer = "My Consumer"
  val provider = "My Provider"

  uponReceiving("a request for foo")
    .matching(path = "/foo")
    .willRespondWith(body = "{}")
  .during { (providerConfig, ee: ExecutionEnv) =>
    Await.result(ConsumerService(providerConfig.url).simpleGet("/foo"), Duration(100, MILLISECONDS)) must beEqualTo(201, Some("{}"))
  }

}
