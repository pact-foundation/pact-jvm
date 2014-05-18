package au.com.dius.pact.consumer

import scala.util.Failure
import scala.util.Success
import scala.util.Try

object VerificationResult {
  def apply(r: Try[PactSessionResults]): VerificationResult = r match {
    case Success(results) if results.allMatched => PactVerified
    case Success(results) => PactMismatch(results)
    case Failure(error) => PactError(error)
  }
}

sealed trait VerificationResult {
  // Temporary.  Should belong somewhere else.
  override def toString(): String = this match {
    case PactVerified => "Pact verified."
    case PactMismatch(results) => s"""
      |Missing: ${results.missing.map(_.request)}\n
      |Unexpected: ${results.unexpected}\n"""
    case PactError(error) => s"${error.getClass.getName} ${error.getMessage}"
  }
}
object PactVerified extends VerificationResult
case class PactMismatch(results: PactSessionResults) extends VerificationResult
case class PactError(error: Throwable) extends VerificationResult
