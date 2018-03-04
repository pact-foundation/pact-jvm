package au.com.dius.pact.model

import au.com.dius.pact.consumer._

/**
  * @deprecated Moved to Kotlin implementation: Use Pact interface instead
  */
@Deprecated
case class PactFragment(consumer: Consumer,
                        provider: Provider,
                        interactions: Seq[RequestResponseInteraction]) {
  import scala.collection.JavaConversions._
  def toPact = new RequestResponsePact(provider, consumer, interactions)

  def duringConsumerSpec[T](config: MockProviderConfig)(test: => T, verification: ConsumerTestVerification[T]): VerificationResult = {
    val server = DefaultMockProvider(config)
    new ConsumerPactRunner(server).runAndWritePact(toPact, config.getPactVersion)(test, verification)
  }

  //TODO: it would be a good idea to ensure that all interactions in the fragment have the same state
  //      really? why?
  def defaultState: Option[String] = interactions.headOption.map(_.getProviderState)

  def runConsumer(config: MockProviderConfig, test: TestRun): VerificationResult = {
    duringConsumerSpec(config)(test.run(config), (u:Unit) => None)
  }

  def description = s"Consumer '${consumer.getName}' has a pact with Provider '${provider.getName}': " +
    interactions.map { i => i.getDescription }.mkString(" and ") + sys.props("line.separator")

}

/**
  * @deprecated Moved to Kotlin implementation
  */
@Deprecated
object PactFragment {
  def consumer(consumer: String) = {
    PactFragmentBuilder.apply(new Consumer(consumer))
  }
}
