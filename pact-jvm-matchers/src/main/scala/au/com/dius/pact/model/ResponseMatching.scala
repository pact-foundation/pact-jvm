package au.com.dius.pact.model

sealed trait ResponseMatch
case object FullResponseMatch extends ResponseMatch
case class ResponseMismatch(mismatches: Seq[ResponsePartMismatch]) extends ResponseMatch

object ResponseMatching extends ResponseMatching(true)

class ResponseMatching(val allowUnexpectedKeys: Boolean) {
  import au.com.dius.pact.model.Matching._

  def matchRules(expected: Response, actual: Response): ResponseMatch = {
    val mismatches = responseMismatches(expected, actual)
    if (mismatches.isEmpty) FullResponseMatch
    else ResponseMismatch(mismatches)
  }
  
  def responseMismatches(expected: Response, actual: Response): Seq[ResponsePartMismatch] = {
    (matchStatus(expected.getStatus, actual.getStatus)
      ++ matchHeaders(expected, actual)
      ++ matchBody(expected, actual, allowUnexpectedKeys)).toSeq
  }
}
