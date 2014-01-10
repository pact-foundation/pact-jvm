package com.dius.pact.consumer

import com.dius.pact.model._

object PactVerification {

  trait VerificationResult
  case class PactFailure(missing: Seq[Interaction], unexpected: Seq[Interaction]) extends VerificationResult
  case object PactVerified extends VerificationResult
  case class MissingInteractions(missing: Seq[Interaction]) extends VerificationResult
  case class UnexpectedInteractions(unexpected: Seq[Interaction]) extends VerificationResult
  case object ConsumerTestsFailed extends VerificationResult

  case class ComposableVerification(o: VerificationResult) {
    def and (v: VerificationResult) = { (o, v) match {
      case (PactVerified, PactVerified) => PactVerified
      case (MissingInteractions(a), UnexpectedInteractions(b)) => PactFailure(a, b)
      case (MissingInteractions(a), _) => MissingInteractions(a)
      case (_, UnexpectedInteractions(b)) => UnexpectedInteractions(b)
    }}

  }
  implicit def composable(a: VerificationResult) = ComposableVerification(a)

  def apply(expected: Seq[Interaction], actual: Seq[Interaction])(testResult: Boolean): VerificationResult = {
    if(!testResult) {
      ConsumerTestsFailed
    } else {
      allExpectedInteractions(expected, actual) and noUnexpectedInteractions(Response.invalidRequest, actual)
    }
  }

  def noUnexpectedInteractions(invalid: Response, actual: Seq[Interaction]): VerificationResult = {
    val unexpected = actual.filter(_.response == invalid)
    if(unexpected.isEmpty) {
      PactVerified
    } else {
      UnexpectedInteractions(unexpected)
    }
  }

  def allExpectedInteractions(expected: Seq[Interaction], actual: Seq[Interaction]): VerificationResult = {
    def in(f: Seq[Interaction])(i:Interaction): Boolean = {
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
