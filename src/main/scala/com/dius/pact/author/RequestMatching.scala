package com.dius.pact.author

import com.dius.pact.model.{Request, Pact, Response}
import scala.util.{Failure, Try}

case class RequestMatching(pact:Pact) {
  def matchRequest(actual:Request):Try[Response] = {
    Failure(new Exception("didn't find it"))
  }
}

object RequestMatching {
  implicit def pimpPactWithRequestMatch(pact:Pact) = RequestMatching(pact)
}
