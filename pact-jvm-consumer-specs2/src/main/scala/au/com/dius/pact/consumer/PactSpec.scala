package au.com.dius.pact.consumer

import au.com.dius.pact.model.PactFragmentBuilder.PactWithAtLeastOneRequest
import au.com.dius.pact.model.{MockProviderConfig, PactFragment, PactSpecVersion}
import org.specs2.execute.{AsResult, Failure, Result, Success}
import org.specs2.specification.create.FragmentsFactory

trait PactSpec extends FragmentsFactory {

  val provider: String
  val consumer: String
  val providerState: Option[String] = None

  def uponReceiving(description: String) = {
    val pact = PactFragment.consumer(consumer).hasPactWith(provider)
    if (providerState.isDefined) pact.given(providerState.get).uponReceiving(description)
    else pact.uponReceiving(description)
  }

  implicit def liftFragmentBuilder(builder: PactWithAtLeastOneRequest): ReadyForTest = {
    new ReadyForTest(PactFragment(builder.consumer, builder.provider, builder.interactions))
  }

  implicit def pactVerificationAsResult: AsResult[VerificationResult] = {
    new AsResult[VerificationResult] {
      def asResult(test: => VerificationResult): Result = {
        test match {
          case PactVerified => Success()
          case PactMismatch(results, error) => Failure(PrettyPrinter.print(results))
          case UserCodeFailed(e) => Failure(m = s"The user code failed: $e")
          case PactError(e) => Failure(m = s"There was an unexpected exception: ${e.getMessage}", stackTrace = e.getStackTrace.toList)
        }
      }
    }
  }

  class ReadyForTest(pactFragment: PactFragment) {
    def withConsumerTest(test: MockProviderConfig => Result) = {
      val config = MockProviderConfig.createDefault(PactSpecVersion.V2)
      val description = s"Consumer '${pactFragment.consumer.getName}' has a pact with Provider '${pactFragment.provider.getName}': " +
        pactFragment.interactions.map { i => i.getDescription }.mkString(" and ") + sys.props("line.separator")

      fragmentFactory.example(description, {
        pactFragment.duringConsumerSpec(config)(test(config), verify)
      })
    }
  }

  case class ConsumerTestFailed(r:Result) extends RuntimeException

  def verify: ConsumerTestVerification[Result] = { r: Result =>
    if(r.isSuccess) {
      None
    } else {
      Some(r)
    }
  }

}
