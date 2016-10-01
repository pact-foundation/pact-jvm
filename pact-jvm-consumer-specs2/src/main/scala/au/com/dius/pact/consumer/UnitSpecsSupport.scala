package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.specs2.VerificationResultAsResult
import au.com.dius.pact.model.{MockProviderConfig, PactFragment, PactSpecVersion}
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments

trait UnitSpecsSupport extends Specification {

  def pactFragment: PactFragment

  protected lazy val pact = pactFragment.toPact
  protected val providerConfig = MockProviderConfig.createDefault(PactSpecVersion.V2)
  protected val server = DefaultMockProvider(providerConfig)
  protected val consumerPactRunner = new ConsumerPactRunner(server)

  override def map(fragments: => Fragments) = {
    step(server.start(pact)) ^
      fragments ^
      step(server.stop()) ^
      fragmentFactory.example(
        "Should match all mock server records",
        VerificationResultAsResult(consumerPactRunner.writePact(pact, PactSpecVersion.V2))
      )
  }
}
