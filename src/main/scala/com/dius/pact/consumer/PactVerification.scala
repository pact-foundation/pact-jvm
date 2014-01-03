package com.dius.pact.consumer

import com.dius.pact.model._
import com.dius.pact.author.FakeProviderServer
import scala.concurrent.Future

object PactVerification {
  def apply(pact: Pact, server: FakeProviderServer): Future[PactVerification] = {
    //TODO: verify pact properly
    Future.successful(PactVerified)
  }


  trait PactVerification
  case object PactVerified extends PactVerification
  case class PactFailure(report: String) extends PactVerification
}
