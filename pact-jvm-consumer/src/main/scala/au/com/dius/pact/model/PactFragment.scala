package au.com.dius.pact.model

import au.com.dius.pact.consumer.DefaultMockProvider
import au.com.dius.pact.consumer.ConsumerPactRunner
import au.com.dius.pact.consumer.VerificationResult

case class PactFragment(consumer: Consumer,
                        provider: Provider,
                        interactions: Seq[Interaction]) {
  
  def toPact: Pact = Pact(provider, consumer, interactions)

  def duringConsumerSpec(config: MockProviderConfig)(test: => Unit): VerificationResult = {
    val server = DefaultMockProvider(config)
    new ConsumerPactRunner(server).runAndWritePact(toPact)(test)
  }

  //TODO: it would be a good idea to ensure that all interactions in the fragment have the same state
  def defaultState: Option[String] = interactions.headOption.map(_.providerState)

  def runConsumer(config: MockProviderConfig, test: Runnable): VerificationResult = {
    duringConsumerSpec(config)(test.run())
  }
}

object PactFragment {
  def consumer(consumer: String) = {
    PactFragmentBuilder.apply(Consumer(consumer))
  }
}

