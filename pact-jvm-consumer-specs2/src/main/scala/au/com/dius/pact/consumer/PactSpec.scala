package au.com.dius.pact.consumer

import au.com.dius.pact.model.PactFragmentBuilder.PactWithAtLeastOneRequest
import au.com.dius.pact.model.{MockProviderConfig, PactFragment}
import org.specs2.SpecificationLike
import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.{AsResult, Failure, Result}
import org.specs2.specification.core.{Execution, Fragments}

trait PactSpec extends SpecificationLike {

  val provider: String
  val consumer: String
  val providerState: String = ""

  var fs = Fragments()

  def is = fragmentsAsSpecStructure(fs)

  def uponReceiving(description: String) = {
    PactFragment.consumer(consumer).hasPactWith(provider).given(providerState).uponReceiving(description)
  }

  implicit def liftFragmentBuilder(builder: PactWithAtLeastOneRequest): ReadyForTest = {
    new ReadyForTest(PactFragment(builder.consumer, builder.provider, builder.interactions))
  }

  class ReadyForTest(pactFragment: PactFragment) {
    def during(test: (MockProviderConfig, ExecutionEnv) => Result) = {
      val config = MockProviderConfig.createDefault()
      val description = pactFragment.interactions.map(i => s"${i.providerState} ${i.description}").mkString(" ")

      fs ^ fragmentFactory.example(description, Execution.withExecutionEnv { ee: ExecutionEnv =>
        val result = pactFragment.duringConsumerSpec(config)(test(config, ee), verify)
        result match {
          case PactVerified => success
          case PactMismatch(results, error) => Failure(PrettyPrinter.print(results))
          case UserCodeFailed(r:Result) => r
          case PactError(e) => Failure(m = s"There was an unexpected exception: ${e.getMessage}", stackTrace = e.getStackTrace.toList)
        }
      })
    }
  }

  case class ConsumerTestFailed(r:Result) extends RuntimeException

  def verify:ConsumerTestVerification[Result] = { r:Result =>
    if(r.isSuccess) {
      None
    } else {
      Some(r)
    }
  }
}
