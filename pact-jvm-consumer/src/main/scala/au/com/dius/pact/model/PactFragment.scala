package au.com.dius.pact.model

import au.com.dius.pact.consumer._

case class PactFragment(consumer: Consumer,
                        provider: Provider,
                        interactions: Seq[RequestResponseInteraction]) {
  import scala.collection.JavaConversions._
  def toPact: RequestResponsePact = new RequestResponsePact(provider, consumer, interactions)

  def duringConsumerSpec[T](config: MockProviderConfig)(test: => T, verification: ConsumerTestVerification[T]): VerificationResult = {
    val server = DefaultMockProvider(config)
    new ConsumerPactRunner(server).runAndWritePact(toPact, config.pactConfig)(test, verification)
  }

  //TODO: it would be a good idea to ensure that all interactions in the fragment have the same state
  //      really? why?
  def defaultState: Option[String] = interactions.headOption.map(_.getProviderState)

  def runConsumer(config: MockProviderConfig, test: TestRun): VerificationResult = {
    duringConsumerSpec(config)(test.run(config), (u:Unit) => None)
  }
}

object PactFragment {
  def consumer(consumer: String) = {
    PactFragmentBuilder.apply(new Consumer(consumer))
  }
}
