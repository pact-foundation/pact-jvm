package au.com.dius.pact.server

import au.com.dius.pact.core.model.RequestResponseInteraction

import scala.collection.JavaConverters._
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
  override def toString() = this match {
    case PactVerified => "Pact verified."
    case PactMismatch(results, error) => s"""
                                            |Missing: ${results.getMissing.asScala.map(_.asInstanceOf[RequestResponseInteraction].getRequest)}\n
                                            |AlmostMatched: ${results.getAlmostMatched.asScala}\n
                                            |Unexpected: ${results.getUnexpected.asScala}\n"""
    case PactError(error) => s"${error.getClass.getName} ${error.getMessage}"
    case UserCodeFailed(error) => s"${error.getClass.getName} $error"
  }
}

object PactVerified extends VerificationResult

case class PactMismatch(results: PactSessionResults, userError: Option[Throwable] = None) extends VerificationResult {
  override def toString() = {
    var s = "Pact verification failed for the following reasons:\n"
    for (mismatch <- results.getAlmostMatched.asScala) {
      s += mismatch.description()
    }
    if (results.getUnexpected.asScala.nonEmpty) {
      s += "\nThe following unexpected results were received:\n"
      for (unexpectedResult <- results.getUnexpected.asScala) {
        s += unexpectedResult.toString()
      }
    }
    if (results.getMissing.asScala.nonEmpty) {
      s += "\nThe following requests were not received:\n"
      for (unexpectedResult <- results.getMissing.asScala) {
        s += unexpectedResult.toString()
      }
    }
    s
  }
}

case class PactError(error: Throwable) extends VerificationResult

case class UserCodeFailed[T](error: T) extends VerificationResult
