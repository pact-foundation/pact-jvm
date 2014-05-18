package au.com.dius.pact.consumer

import au.com.dius.pact.model.{MockProviderConfig, PactFragment}
import org.specs2.SpecificationLike
import org.specs2.specification._
import org.specs2.matcher.{StandardMatchResults, MustMatchers}
import org.specs2.execute.{Result, StandardResults}
import au.com.dius.pact.model.PactFragmentBuilder.PactWithAtLeastOneRequest

trait PactSpec extends SpecificationLike
  with MustMatchers
  with StandardResults
  with StandardMatchResults
  with FragmentsBuilder {

  var fragments = Seq[Fragment]()

  val provider: String
  val consumer: String
  val providerState: String = ""

  override def is: Fragments = Fragments.create(fragments :_*)

  def uponReceiving(description: String) = {
    PactFragment.consumer(consumer).hasPactWith(provider).given(providerState).uponReceiving(description)
  }

  implicit def liftFragmentBuilder(builder: PactWithAtLeastOneRequest): ReadyForTest = {
    new ReadyForTest(PactFragment(builder.consumer, builder.provider, builder.interactions))
  }

  class ReadyForTest(fragment: PactFragment) {
    def during(test: MockProviderConfig => Result) = {
      val config = MockProviderConfig.createDefault()
      val description = fragment.interactions.map(i => s"${i.providerState} ${i.description}").mkString(" ")

      fragments = fragments :+ Example(description, {
        fragment.duringConsumerSpec(config)(test(config)) must beEqualTo(PactVerified)
      })
    }
  }
}
