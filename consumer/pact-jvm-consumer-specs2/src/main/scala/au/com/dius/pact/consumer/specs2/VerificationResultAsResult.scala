package au.com.dius.pact.consumer.specs2

import au.com.dius.pact.consumer.PactVerificationResult.{Error, ExpectedButNotReceived, Mismatches, Ok, PartialMismatch, UnexpectedRequest}
import au.com.dius.pact.consumer._
import org.specs2.execute._

import scala.collection.JavaConverters._

object VerificationResultAsResult {

  def apply(t: => PactVerificationResult): Result = {
    t match {
      case r: Ok => r.getResult.asInstanceOf[Result]
      case r: PartialMismatch => Failure(PrettyPrinter.printProblem(r.getMismatches.asScala))
      case e: Mismatches => Failure(PrettyPrinter.print(e.getMismatches.asScala))
      case e: Error => Failure(m = s"Test failed with an exception: ${e.getError.getMessage}",
        stackTrace = e.getError.getStackTrace.toList)
      case u: UnexpectedRequest => Failure(PrettyPrinter.printUnexpected(List(u.getRequest)))
      case u: ExpectedButNotReceived => Failure(PrettyPrinter.printMissing(u.getExpectedRequests.asScala))
    }
  }
}
