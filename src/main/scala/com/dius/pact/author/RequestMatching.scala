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
        matchHeaders(request.headers, actual.headers) &&
        matchBodies(request.body, actual.body)
    }.fold[Either[Response, String]](Right(s"unexpected request")) {i => Left(i.response)}
  }

  def matchHeaders(expected: Option[Map[String, String]], actual: Option[Map[String, String]]):Boolean = {
    (expected, actual) match {
      case (None, None) => true
      case (None, _) => false
      case (_, None) => false
      case (Some(a), Some(b)) => a == b.filter { case (k, _) => a.contains(k) }
    }
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
