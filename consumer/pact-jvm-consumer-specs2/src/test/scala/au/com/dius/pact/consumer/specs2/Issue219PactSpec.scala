package au.com.dius.pact.consumer.specs2

import java.util.concurrent.TimeUnit.MILLISECONDS

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class Issue219PactSpec extends Specification with PactSpec {

  val consumer = "My Consumer"
  val provider = "My Provider"

  val timeout = Duration(1000, MILLISECONDS)

  override def is = uponReceiving("add a broker")
    .matching(path = "/api/broker/add", query = "options=delete.topic.enable%3Dtrue&broker=1")
    .willRespondWith(maybeBody = Some("{}"))
    .withConsumerTest((mockServer, _) => {
      val get = ConsumerService(mockServer.getUrl).simpleGet("/api/broker/add", "options=delete.topic.enable%3Dtrue&broker=1")
      Await.result(get, timeout) must be_==(200, "{}")
    })

}
