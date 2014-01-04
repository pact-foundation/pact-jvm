package com.dius.pact.model

import org.json4s.Diff
import com.dius.pact.model.JsonDiff._

object Matching {
  sealed trait MatchResult {
    def and(o: MatchResult): MatchResult = { (this, o) match {
      case (MatchFound, MatchFound) => MatchFound
      case (a, MatchFound) => a
      case (MatchFound, b) => b
      case (MatchFailure(a), MatchFailure(b)) => MatchFailure(a.reverse_:::(b))
      case (a, MatchFailure(b)) => MatchFailure(b :+ a)
      case (MatchFailure(a), b) => MatchFailure(a :+ b)
      case (a, b) => MatchFailure(List(a, b))
    }}
  }

  case object MatchFound extends MatchResult
  case class MatchFailure(problems: List[MatchResult]) extends MatchResult
  case class MethodMismatch(expected: HttpMethod, actual: HttpMethod) extends MatchResult
  case class PathMismatch(expected: String, actual: String) extends MatchResult
  case class HeaderMismatch(expected: Headers, actual: Headers) extends MatchResult
  case class BodyContentMismatch(diff: Diff) extends MatchResult

  implicit def pimpPactWithRequestMatch(pact: Pact) = RequestMatching(pact.interactions)

  private type Headers = Option[Map[String, String]]

  def compareHeaders(a: Map[String, String], b: Map[String, String]): MatchResult = {
    val relevantActualHeaders = b.filter { case (key, _) => a.contains(key) }
    if(a == relevantActualHeaders) {
      MatchFound
    } else {
      HeaderMismatch(Some(a), Some(relevantActualHeaders))
    }
  }

  def matchHeaders(expected: Headers, actual: Headers, reverseHeaders: Boolean = false): MatchResult = {
    val e = expected.getOrElse(Map())
    val a = actual.getOrElse(Map())
    if(reverseHeaders) {
      compareHeaders(a, e)
    } else {
      compareHeaders(e, a)
    }
  }

  def matchMethod(expected: HttpMethod, actual: HttpMethod): MatchResult = {
    if(expected == actual) {
      MatchFound
    } else {
      MethodMismatch(expected, actual)
    }
  }

  private type Body = Option[String]

  def matchBodies(expected: Body, actual: Body, diffConfig: DiffConfig): MatchResult = {
    val d = diff(expected.getOrElse(""), actual.getOrElse(""), diffConfig)
    if(d == noChange) {
      MatchFound
    } else {
      BodyContentMismatch(d)
    }
  }

  def matchPath(expected: String, actual: String): MatchResult = {
    val pathFilter = "http[s]*://([^/]*)"
    val replacedActual = actual.replaceFirst(pathFilter, "")
    if(expected == replacedActual) {
      MatchFound
    } else {
      PathMismatch(expected, replacedActual)
    }
  }
}
