package au.com.dius.pact.model

import au.com.dius.pact.model.JsonDiff.DiffConfig

sealed trait ResponseMatch
case object FullResponseMatch extends ResponseMatch
case object ResponseMismatch extends ResponseMatch

object ResponseMatching extends ResponseMatching(DiffConfig(allowUnexpectedKeys = true, structural = false))

class ResponseMatching(val providerDiffConfig: DiffConfig) {
  import Matching._

  def matchRules(expected: Response, actual: Response): ResponseMatch = 
    if (responseMismatches(expected, actual).isEmpty) FullResponseMatch
    else ResponseMismatch
  
  def responseMismatches(expected: Response, actual: Response): Seq[ResponsePartMismatch] = {
    (matchStatus(expected.status, actual.status) 
      ++ matchHeaders(expected.headers, actual.headers) 
      ++ matchBody(expected.mimeType, expected.body, actual.mimeType, actual.body, providerDiffConfig)).toSeq
  }
}
