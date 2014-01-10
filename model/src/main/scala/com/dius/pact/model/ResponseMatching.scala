package com.dius.pact.model

import com.dius.pact.model.JsonDiff.DiffConfig

object ResponseMatching {
  import Matching._
  val providerDiffConfig = new DiffConfig(allowUnexpectedKeys = true, structural = false)

  def matchRules(expected: Response, actual: Response): MatchResult = {
    matchStatus(expected.status, actual.status) and
      matchHeaders(expected.headers, actual.headers) and
      matchBodies(expected.body, actual.body, providerDiffConfig)
  }
}
