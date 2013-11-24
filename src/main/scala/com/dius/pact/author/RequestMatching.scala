package com.dius.pact.author

import com.dius.pact.model.{Request, Pact, Response}

case class RequestMatching(pact:Pact) {
  def matchRequest(actual:Request):Option[Response] = {
    None
  }
}

object RequestMatching {
  implicit def pimpPactWithRequestMatch(pact:Pact) = RequestMatching(pact)
}
