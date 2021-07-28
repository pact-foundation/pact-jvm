package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.IRequest
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.SynchronousRequestResponse
import mu.KLogging

sealed class RequestMatch {
  private val score: Int
    get() {
      return when (this) {
        is FullRequestMatch -> this.calculateScore()
        is PartialRequestMatch -> this.calculateScore()
        else -> 0
      }
    }

  /**
   * Take the first total match, or merge partial matches, or take the best available.
   */
  fun merge(other: RequestMatch): RequestMatch = when {
    this is FullRequestMatch && other is FullRequestMatch -> if (this.score >= other.score) this
      else other
    this is FullRequestMatch -> this
    other is FullRequestMatch -> other
    this is PartialRequestMatch && other is PartialRequestMatch -> if (this.score >= other.score) this
      else other
    this is PartialRequestMatch -> this
    else -> other
  }
}

data class FullRequestMatch(
  val interaction: SynchronousRequestResponse,
  val result: RequestMatchResult
) : RequestMatch() {
  fun calculateScore() = result.calculateScore()
}

data class PartialRequestMatch(val problems: Map<Interaction, RequestMatchResult>) : RequestMatch() {
  fun description(): String {
    var s = ""
    for (problem in problems) {
      s += problem.key.description + ":\n"
      for (mismatch in problem.value.mismatches) {
        s += "    " + mismatch.description() + "\n"
      }
    }
    return s
  }

  fun calculateScore() = problems.values.map { it.calculateScore() }.maxOrNull() ?: 0
}

object RequestMismatch : RequestMatch()

class RequestMatching(private val expectedInteractions: List<Interaction>) {
  fun matchInteraction(actual: IRequest): RequestMatch {
    val matches = expectedInteractions
      .filter { it.isSynchronousRequestResponse() }
      .map { compareRequest(it.asSynchronousRequestResponse()!!, actual) }
    return if (matches.isEmpty())
      RequestMismatch
    else
      matches.reduce { acc, match -> acc.merge(match) }
  }

  fun findResponse(actual: IRequest): Response? {
    return when (val match = matchInteraction(actual)) {
      is FullRequestMatch -> (match.interaction as RequestResponseInteraction).response
      else -> null
    }
  }

  companion object : KLogging() {
    private fun decideRequestMatch(expected: SynchronousRequestResponse, result: RequestMatchResult) =
      when {
        result.matchedOk() -> FullRequestMatch(expected, result)
        result.matchedMethodAndPath() -> PartialRequestMatch(mapOf(expected to result))
        else -> RequestMismatch
      }

    fun compareRequest(expected: SynchronousRequestResponse, actual: IRequest): RequestMatch {
        val mismatches = requestMismatches(expected.request, actual)
        logger.debug { "Request mismatch: $mismatches" }
        return decideRequestMatch(expected, mismatches)
    }

    @JvmStatic
    fun requestMismatches(expected: IRequest, actual: IRequest): RequestMatchResult {
      logger.debug { "comparing to expected request: \n$expected" }

      val pathContext = MatchingContext(expected.matchingRules.rulesForCategory("path"), false)
      val bodyContext = MatchingContext(expected.matchingRules.rulesForCategory("body"), false)
      val queryContext = MatchingContext(expected.matchingRules.rulesForCategory("query"), false)
      val headerContext = MatchingContext(expected.matchingRules.rulesForCategory("header"), false)

      return RequestMatchResult(Matching.matchMethod(expected.method, actual.method),
        Matching.matchPath(expected, actual, pathContext),
        Matching.matchQuery(expected, actual, queryContext),
        Matching.matchCookies(expected.cookies(), actual.cookies(), headerContext),
        Matching.matchRequestHeaders(expected, actual, headerContext),
        Matching.matchBody(expected.asHttpPart(), actual.asHttpPart(), bodyContext))
    }
  }
}
