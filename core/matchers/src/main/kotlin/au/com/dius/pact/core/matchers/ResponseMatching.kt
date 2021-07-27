package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.IResponse
import mu.KLogging

sealed class ResponseMatch
object FullResponseMatch : ResponseMatch()
data class ResponseMismatch(val mismatches: List<Mismatch>) : ResponseMatch()

object ResponseMatching : KLogging() {

  @JvmStatic
  fun matchRules(expected: IResponse, actual: IResponse): ResponseMatch {
    val mismatches = responseMismatches(expected, actual)
    return if (mismatches.isEmpty()) FullResponseMatch
    else ResponseMismatch(mismatches)
  }

  @JvmStatic
  fun responseMismatches(expected: IResponse, actual: IResponse): List<Mismatch> {
    val statusContext = MatchingContext(expected.matchingRules.rulesForCategory("status"), true)
    val bodyContext = MatchingContext(expected.matchingRules.rulesForCategory("body"), true)
    val headerContext = MatchingContext(expected.matchingRules.rulesForCategory("header"), true)

    val bodyResults = Matching.matchBody(expected.asHttpPart(), actual.asHttpPart(), bodyContext)
    val typeResult = if (bodyResults.typeMismatch != null) {
      listOf(bodyResults.typeMismatch)
    } else {
      emptyList()
    }
    return (typeResult + Matching.matchStatus(expected.status, actual.status, statusContext) +
      Matching.matchHeaders(expected.asHttpPart(), actual.asHttpPart(), headerContext).flatMap { it.result } +
      bodyResults.bodyResults.flatMap { it.result }).filterNotNull()
  }
}
