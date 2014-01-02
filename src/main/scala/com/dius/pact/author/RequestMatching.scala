package com.dius.pact.author

import com.dius.pact.model.{Request, Pact, Response}
import com.dius.pact.model.JsonDiff._

case class MatchFailure(msg: String) extends Exception(msg)

case class RequestMatching(pact: Pact) {
  val diffConfig = DiffConfig(allowUnexpectedKeys = false)

  def matchRequest(actual: Request): Either[Response, String] = {
    val pathFilter = "http[s]*://([^/]*)"
    pact.interactions.find { i =>
      val request = i.request
      request.method == actual.method &&
        request.path == actual.path.replaceFirst(pathFilter, "") &&
//    TODO: implement header matching
//        request.headers == actual.headers &&
        matchBodies(request.body, actual.body)
    }.fold[Either[Response, String]](Right(s"unexpected request $actual")) {i => Left(i.response)}
  }

  private def matchBodies(a: Option[String], b: Option[String]): Boolean = {
    (a, b) match {
      case (None, None) => true
      case (None, _) => false
      case (_, None) => false
      case (Some(aa), Some(bb)) => matches(aa, bb, diffConfig)
    }
  }
}

object RequestMatching {
  implicit def pimpPactWithRequestMatch(pact:Pact) = RequestMatching(pact)
}
