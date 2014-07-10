package au.com.dius.pact.model

import au.com.dius.pact.consumer.{ConsumerTestVerification, DefaultMockProvider, ConsumerPactRunner, VerificationResult}

case class PactFragment(consumer: Consumer,
                        provider: Provider,
                        interactions: Seq[Interaction]) {
  
  def toPact: Pact = Pact(provider, consumer, interactions)

  def duringConsumerSpec[T](config: MockProviderConfig)(test: => T, verification: ConsumerTestVerification[T]): VerificationResult = {
    val server = DefaultMockProvider(config)
    new ConsumerPactRunner(server).runAndWritePact(toPact)(test, verification)
  }

  //TODO: it would be a good idea to ensure that all interactions in the fragment have the same state
  def defaultState: Option[String] = interactions.headOption.map(_.providerState).get

  def runConsumer(config: MockProviderConfig, test: Runnable): VerificationResult = {
    duringConsumerSpec(config)(test.run(), (u:Unit) => None)
  }
}

object PactFragment {
  def consumer(consumer: String) = {
    PactFragmentBuilder.apply(Consumer(consumer))
  }
}

