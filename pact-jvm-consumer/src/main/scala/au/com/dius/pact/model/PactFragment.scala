package au.com.dius.pact.model

import au.com.dius.pact.consumer.{ConsumerPact, PactVerification, MockProviderConfig}
import au.com.dius.pact.model.HttpMethod._
import org.json4s._
import scala.Some
import scala.concurrent.{Future, Await}

case class PactFragment(consumer: Consumer,
                        provider: Provider,
                        state: Option[String],
                        description: String,
                        request: Request,
                        response: Response) {
  def duringConsumerSpec(test: => Unit, config: MockProviderConfig = MockProviderConfig()): Future[PactVerification.VerificationResult] = {
    //TODO: State needs to be a proper option all through the domain
    val defaultState = state.getOrElse("")
    //TODO: ConsumerPact should no longer be necessary, move implementation into this file
    val interaction = Interaction(defaultState, description, request, response)
    val pact = Pact(provider, consumer, List(interaction))
    ConsumerPact(pact).runConsumer(config, defaultState)(test)
  }

  def runConsumer(config: MockProviderConfig, test: Runnable): PactVerification.VerificationResult = {
    import scala.concurrent.duration._
    Await.result(duringConsumerSpec({ config:MockProviderConfig => test.run() }, config), 20 seconds)
  }
}

object PactFragment {
  def consumer(consumer: String) = {
    PactWithConsumer(Consumer(consumer))
  }
}

case class PactWithConsumer(consumer: Consumer) {
  def hasPactWith(provider: String) = {
    PactWithProvider(consumer, Provider(provider))
  }
}

case class PactWithProvider(consumer: Consumer, provider: Provider) {
  def given(state: String) = {
    PactInState(consumer, provider, Some(state))
  }

  def uponReceiving(description: String) = {
    PactInState(consumer, provider, None).uponReceiving(description)
  }
}

case class PactInState(consumer: Consumer, provider: Provider, state: Option[String]) {
  def uponReceiving(description: String) = {
    DescribedRequest(consumer, provider, state, description)
  }
}

case class DescribedRequest(consumer: Consumer, provider: Provider, state: Option[String], description: String) extends Optionals {

  /**
   * supports java DSL
   */
  def matching(path: String, method: String, headers: java.util.Map[String, String], body: String): PactForRequest = {
    import collection.JavaConversions._
    matching(path, method, optional(headers.toMap), optional(body))
  }

  def matching(path: String,
                 method: String = Get,
                 headers: Option[Map[String, String]] = None,
                 body: Option[JValue] = None): PactForRequest = {
    new PactForRequest(consumer, provider, state, description, Request(method, path, headers, body))
  }
}

class PactForRequest(consumer: Consumer,
                     provider: Provider,
                     state: Option[String],
                     description: String,
                     request: Request) {
  /**
   * supports java DSL
   */
  def willRespondWith(status: Int, headers: java.util.Map[String, String], body: String): PactFragment = {
    import collection.JavaConversions._
    willRespondWith(status, headers.toMap, body)
  }

  def willRespondWith(status:Int = 200,
                      headers: Map[String,String] = Map(),
                      body: String = ""): PactFragment = {
    PactFragment(consumer,
      provider,
      state,
      description,
      request,
      Response(status, headers, body))
  }
}