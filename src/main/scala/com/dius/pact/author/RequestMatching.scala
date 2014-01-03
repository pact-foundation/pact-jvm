package com.dius.pact.author

import com.dius.pact.model._
import com.dius.pact.model.JsonDiff._
import com.dius.pact.model.Pact
import com.dius.pact.model.JsonDiff.DiffConfig
import com.dius.pact.model.Interaction
import com.dius.pact.model.Request
import scala.Some


case class MatchFailure(msg: String) extends Exception(msg)

//TODO: find a better way to handle the header reverse thing
case class RequestMatching(interactions: Seq[Interaction], reverseHeaders: Boolean = false) {
  val diffConfig = DiffConfig(allowUnexpectedKeys = false)

  def matchRequest(actual: Request): Option[Response] = {
    interactions.find(matchRules(actual)).fold[Option[Response]](None) {i => Some(i.response)}
  }

  def matchRules(actual: Request)(i:Interaction): Boolean = {
    val request = i.request

    matchMethod(request.method, actual.method) &&
      matchPath(request.path, actual.path) &&
      matchHeaders(request.headers, actual.headers) &&
      matchBodies(request.body, actual.body)
  }

  def matchMethod(expected: HttpMethod, actual: HttpMethod): Boolean = {
//    println(s"$expected == $actual ${expected == actual}")
    expected == actual
  }

  def matchPath(expected: String, actual: String): Boolean = {
    val pathFilter = "http[s]*://([^/]*)"
//    println(s"""$expected == $actual.replaceFirst(pathFilter, "") ${expected == actual.replaceFirst(pathFilter, "")}""")
    expected == actual.replaceFirst(pathFilter, "")
  }

  private type Headers = Option[Map[String, String]]

  def matchHeaders(expected: Headers, actual: Headers):Boolean = {
    def compareHeaders(a: Map[String, String], b: Map[String, String]) = {
      a == b.filter { case (key, _) => a.contains(key) }
    }

    def check(e: Headers, a: Headers) = (e, a) match {
      case (None, None) => true
      case (None, _) => false
      case (_, None) => false
      case (Some(expectedHeaders), Some(actualHeaders)) => {
        if(reverseHeaders) {
          compareHeaders(actualHeaders, expectedHeaders)
        } else {
          compareHeaders(expectedHeaders, actualHeaders)
        }
      }
    }
//    println(s"$expected $actual ${check(expected, actual)}")

    check(expected, actual)
  }


  private type Body = Option[String]

  private def matchBodies(expected: Body, actual: Body): Boolean = {
    def check(a: Body, b: Body) = (a, b) match {
      case (None, None) => true
      case (None, _) => false
      case (_, None) => false
      case (Some(aa), Some(bb)) => matches(aa, bb, diffConfig)
    }

//    println(s"$expected $actual ${check(expected, actual)}")

    check(expected, actual)
  }
}

object RequestMatching {
  implicit def pimpPactWithRequestMatch(pact:Pact) = RequestMatching(pact.interactions)
}
