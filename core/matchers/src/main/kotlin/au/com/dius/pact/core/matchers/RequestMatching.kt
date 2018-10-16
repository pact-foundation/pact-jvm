package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import mu.KLogging

sealed class RequestMatch {
  /**
   * Take the first total match, or merge partial matches, or take the best available.
   */
  fun merge(other: RequestMatch): RequestMatch = when {
    this is FullRequestMatch && other is FullRequestMatch -> this
    this is PartialRequestMatch && other is PartialRequestMatch ->
      PartialRequestMatch(this.problems + other.problems)
    this is FullRequestMatch -> this
    other is FullRequestMatch -> other
    this is PartialRequestMatch -> this
    else -> other
  }
}

data class FullRequestMatch(val interaction: Interaction) : RequestMatch()

data class PartialRequestMatch(val problems: Map<Interaction, List<Mismatch>>) : RequestMatch() {
  fun description(): String {
    var s = ""
    for (problem in problems) {
      s += problem.key.description + ":\n"
      for (mismatch in problem.value) {
        s += "    " + mismatch.description() + "\n"
      }
    }
    return s
  }
}

object RequestMismatch : RequestMatch()

class RequestMatching(private val expectedInteractions: List<RequestResponseInteraction>) {

    fun matchInteraction(actual: Request): RequestMatch {
      val matches = expectedInteractions.map { compareRequest(it, actual) }
      return if (matches.isEmpty())
        RequestMismatch
      else
        matches.reduce { acc, match -> acc.merge(match) }
    }

    fun findResponse(actual: Request): Response? {
      val match = matchInteraction(actual)
      return when (match) {
        is FullRequestMatch -> (match.interaction as RequestResponseInteraction).response
        else -> null
      }
    }

  companion object : KLogging() {
    var allowUnexpectedKeys = false

    //  implicit def liftPactForMatching(pact: RequestResponsePact): RequestMatching =
    //    RequestMatching(JavaConversions.collectionAsScalaIterable(pact.getInteractions).toSeq)

    private fun isPartialMatch(problems: List<Mismatch>): Boolean = !problems.any {
      when (it) {
        is PathMismatch, is MethodMismatch -> true
        else -> false
      }
    }

    private fun decideRequestMatch(expected: RequestResponseInteraction, problems: List<Mismatch>) =
      when {
        problems.isEmpty() -> FullRequestMatch(expected)
        isPartialMatch(problems) -> PartialRequestMatch(mapOf(expected to problems))
        else -> RequestMismatch
      }

    fun compareRequest(expected: RequestResponseInteraction, actual: Request): RequestMatch {
        val mismatches = requestMismatches(expected.request, actual)
        logger.debug { "Request mismatch: $mismatches" }
        return decideRequestMatch(expected, mismatches)
    }

    @JvmStatic
    fun requestMismatches(expected: Request, actual: Request): List<Mismatch> {
      logger.debug { "comparing to expected request: \n$expected" }
      return (listOf(Matching.matchMethod(expected.method, actual.method)) +
        Matching.matchPath(expected, actual) +
        Matching.matchQuery(expected, actual) +
        Matching.matchCookie(expected.cookie() ?: emptyList(), actual.cookie() ?: emptyList()) +
        Matching.matchRequestHeaders(expected, actual) +
        Matching.matchBody(expected, actual, allowUnexpectedKeys)
      ).filterNotNull()
    }
  }
}
