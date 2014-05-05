package au.com.dius.pact.model

import au.com.dius.pact.consumer.{ConsumerPact, PactVerification, MockProviderConfig}
import scala.concurrent.{Future, Await}

case class PactFragment(consumer: Consumer,
                        provider: Provider,
                        interactions: Seq[Interaction]) {
  def duringConsumerSpec(test: MockProviderConfig => Unit, config: MockProviderConfig = MockProviderConfig()): Future[PactVerification.VerificationResult] = {
    val pact = Pact(provider, consumer, interactions)
    ConsumerPact(pact).runConsumer(config, defaultState)({test(config)})
  }

  //TODO: it would be a good idea to ensure that all interactions in the fragment have the same state
  def defaultState = {
    interactions.head.providerState
  }

  def runConsumer(config: MockProviderConfig, test: Runnable): PactVerification.VerificationResult = {
    import scala.concurrent.duration._
    Await.result(duringConsumerSpec({ config:MockProviderConfig => test.run() }, config), 20 seconds)
  }
}

object PactFragment {
  def consumer(consumer: String) = {
    PactFragmentBuilder.apply(Consumer(consumer))
  }
}

