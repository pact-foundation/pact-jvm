package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.Response
import mu.KLogging

sealed class ResponseMatch
object FullResponseMatch : ResponseMatch()
data class ResponseMismatch(val mismatches: List<Mismatch>) : ResponseMatch()

object ResponseMatching : KLogging() {

  @JvmStatic
  fun matchRules(expected: Response, actual: Response): ResponseMatch {
    val mismatches = responseMismatches(expected, actual, true)
    return if (mismatches.isEmpty()) FullResponseMatch
    else ResponseMismatch(mismatches)
  }

  @JvmStatic
  fun responseMismatches(expected: Response, actual: Response, allowUnexpectedKeys: Boolean): List<Mismatch> {
    val bodyResults = Matching.matchBody(expected, actual, allowUnexpectedKeys)
    val typeResult = if (bodyResults.typeMismatch != null) {
      listOf(bodyResults.typeMismatch)
    } else {
      emptyList()
    }
    return (typeResult + Matching.matchStatus(expected.status, actual.status) +
      Matching.matchHeaders(expected, actual).flatMap { it.result } +
      bodyResults.bodyResults.flatMap { it.result }).filterNotNull()
  }
}
