package au.com.dius.pact.model

sealed trait ResponseMatch
case object FullResponseMatch extends ResponseMatch
case object ResponseMismatch extends ResponseMatch

object ResponseMatching extends ResponseMatching(DiffConfig(allowUnexpectedKeys = true, structural = false))

class ResponseMatching(val providerDiffConfig: DiffConfig) {
  import au.com.dius.pact.model.Matching._

  def matchRules(expected: Response, actual: Response): ResponseMatch = 
    if (responseMismatches(expected, actual).isEmpty) FullResponseMatch
    else ResponseMismatch
  
  def responseMismatches(expected: Response, actual: Response): Seq[ResponsePartMismatch] = {
    (matchStatus(expected.status, actual.status) 
      ++ matchHeaders(expected, actual)
      ++ matchBody(expected, actual, providerDiffConfig)).toSeq
  }
}
