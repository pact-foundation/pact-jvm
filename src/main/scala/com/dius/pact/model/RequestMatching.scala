package com.dius.pact.model

import JsonDiff._

//TODO: find a better way to handle the header reverse thing
case class RequestMatching(interactions: Seq[Interaction], reverseHeaders: Boolean = false) {
  import Matching._
  import RequestMatching._

  def findResponse(actual: Request): Option[Response] = {
    interactions.find(matchRules(actual)).fold[Option[Response]](None) {i => Some(i.response)}
  }

  def matchRules(actual: Request)(i:Interaction): Boolean = {
    val request = i.request

    val result = matchMethod(request.method, actual.method) and
      matchPath(request.path, actual.path) and
      matchHeaders(request.headers, actual.headers) and
      matchBodies(request.body, actual.body, diffConfig)

    result == MatchFound
  }
}

object RequestMatching {
  val diffConfig = DiffConfig(allowUnexpectedKeys = false, structural = false)

  implicit def pimpPactWithRequestMatch(pact: Pact) = RequestMatching(pact.interactions)
}

