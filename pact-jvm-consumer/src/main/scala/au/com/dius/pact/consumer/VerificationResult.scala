package au.com.dius.pact.consumer

import au.com.dius.pact.model.RequestResponseInteraction

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

/**
  * @deprecated Moved to Kotlin implementation
  */
@Deprecated
sealed trait VerificationResult {
  // Temporary.  Should belong somewhere else.
  override def toString() = this match {
    case PactVerified => "Pact verified."
    case PactMismatch(results, error) => s"""
      |Missing: ${results.missing.map(_.asInstanceOf[RequestResponseInteraction].getRequest)}\n
      |AlmostMatched: ${results.almostMatched}\n
      |Unexpected: ${results.unexpected}\n"""
    case PactError(error) => s"${error.getClass.getName} ${error.getMessage}"
    case UserCodeFailed(error) => s"${error.getClass.getName} $error"
  }
}

/**
  * @deprecated Moved to Kotlin implementation
  */
@Deprecated
object PactVerified extends VerificationResult
/**
  * @deprecated Moved to Kotlin implementation
  */
@Deprecated
case class PactMismatch(results: PactSessionResults, userError: Option[Throwable] = None) extends VerificationResult {
  override def toString() = {
    var s = "Pact verification failed for the following reasons:\n"
    for (mismatch <- results.almostMatched) {
      s += mismatch.description()
    }
    if (results.unexpected.nonEmpty) {
      s += "\nThe following unexpected results were received:\n"
      for (unexpectedResult <- results.unexpected) {
        s += unexpectedResult.toString()
      }
    }
    if (results.missing.nonEmpty) {
      s += "\nThe following requests were not received:\n"
      for (unexpectedResult <- results.missing) {
        s += unexpectedResult.toString()
      }
    }
    s
  }
}
/**
  * @deprecated Moved to Kotlin implementation
  */
@Deprecated
case class PactError(error: Throwable) extends VerificationResult
/**
  * @deprecated Moved to Kotlin implementation
  */
@Deprecated
case class UserCodeFailed[T](error: T) extends VerificationResult
