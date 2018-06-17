package au.com.dius.pact.consumer.specs2

import au.com.dius.pact.consumer._
import au.com.dius.pact.core.model.RequestResponseInteraction
import org.specs2.execute._

object VerificationResultAsResult {

  def apply(t: => VerificationResult): Result = {
    t match {
      case PactVerified => Success()
      case PactMismatch(results, error) => Failure(s"""
          |Missing: ${results.missing.map(_.asInstanceOf[RequestResponseInteraction].getRequest)}\n
          |AlmostMatched: ${results.almostMatched}\n
          |Unexpected: ${results.unexpected}\n""")
      case PactError(error) => Error(error)
      case UserCodeFailed(error) => Failure(s"${error.getClass.getName} $error")
    }
  }
}
