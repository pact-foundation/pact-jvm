package com.dius.pact.consumer

import com.dius.pact.model._
import scala.util.{Success, Failure, Try}
import com.dius.pact.model.Pact.ConflictingInteractions

object PactVerification {

  trait VerificationResult
  private def logEach(msg: String, iterable: Iterable[AnyRef]): String = {
    s"$msg:${iterable.map("\n" + _)}"
  }

  case class PactFailure(missing: Iterable[Interaction], unexpected: Iterable[Interaction]) extends VerificationResult {
    override def toString: String = {
      "Multiple pact failures\n" +
        logEach("missing interactions:", missing) +
        logEach("unexpected interactions:", unexpected)
    }
  }
  case class PactWritten(destination: String) extends VerificationResult
  case object PactVerified extends VerificationResult
  case class MissingInteractions(missing: Iterable[Interaction]) extends VerificationResult {
    override def toString: String = {
      logEach("missing interactions:", missing)
    }
  }

  case class UnexpectedInteractions(unexpected: Iterable[Interaction]) extends VerificationResult {
    override def toString: String = {
      logEach("unexpected interactions:", unexpected)
    }
  }
  case class ConsumerTestsFailed(error: Throwable) extends VerificationResult
  case class PactMergeFailed(error: ConflictingInteractions) extends VerificationResult {
    override def toString: String = {
      s"This interaction conflicts with others: \n$error"
    }
  }


  case class ComposableVerification(o: VerificationResult) {
    def and (v: VerificationResult) = { (o, v) match {
      case (PactVerified, PactVerified) => PactVerified
      case (MissingInteractions(a), UnexpectedInteractions(b)) => PactFailure(a, b)
      case (MissingInteractions(a), _) => MissingInteractions(a)
      case (_, UnexpectedInteractions(b)) => UnexpectedInteractions(b)
    }}

  }
  implicit def composable(a: VerificationResult) = ComposableVerification(a)

  def apply(expected: Iterable[Interaction], actual: Iterable[Interaction])(testResult: Try[Unit]): VerificationResult = {
    testResult match {
      case Success(_) => {
        val invalidResponse = Response(500, None, None)
        allExpectedInteractions(expected, actual) and noUnexpectedInteractions(invalidResponse, actual)
      }
      case Failure(t) => ConsumerTestsFailed(t)
    }
  }

  def noUnexpectedInteractions(invalid: Response, actual: Iterable[Interaction]): VerificationResult = {
    val unexpected = actual.filter(_.response == invalid)
    if(unexpected.isEmpty) {
      PactVerified
    } else {
      UnexpectedInteractions(unexpected)
    }
  }

  def allExpectedInteractions(expected: Iterable[Interaction], actual: Iterable[Interaction]): VerificationResult = {
    def in(f: Iterable[Interaction])(i:Interaction): Boolean = {
      RequestMatching(f, true).findResponse(i.request).isDefined
    }
    val missing = expected.filterNot(in(actual))
    if(missing.isEmpty) {
      PactVerified
    } else {
      MissingInteractions(missing)
    }
  }

}
