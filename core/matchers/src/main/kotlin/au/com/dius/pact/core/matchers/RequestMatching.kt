package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.IRequest
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.SynchronousRequestResponse
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import io.pact.plugins.jvm.core.PluginConfiguration
import io.github.oshai.kotlinlogging.KLogging

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

class RequestMatching(private val expectedPact: Pact) {
  fun matchInteraction(actual: IRequest): RequestMatch {
    val pluginConfiguration = when (expectedPact) {
      is V4Pact -> expectedPact.pluginData()
      else -> emptyList()
    }
    val matches = expectedPact.interactions
      .filter { it.isSynchronousRequestResponse() }
      .map { interaction ->
        val response = interaction.asSynchronousRequestResponse()!!
        compareRequest(response, actual, pluginConfiguration.associate {
          it.name to PluginConfiguration(
            if (interaction.isV4()) {
              interaction.asV4Interaction().pluginConfiguration[it.name] ?: emptyMap()
            } else {
              emptyMap()
            }.toMutableMap(),
            it.configuration.mapValues { (_, value) -> Json.toJson(value) }.toMutableMap()
          )
        })
      }
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

    @JvmOverloads
    fun compareRequest(
      expected: SynchronousRequestResponse,
      actual: IRequest,
      pluginConfiguration: Map<String, PluginConfiguration> = mapOf()
    ): RequestMatch {
        val mismatches = requestMismatches(expected.request, actual, pluginConfiguration)
        logger.debug { "Request mismatch: $mismatches" }
        return decideRequestMatch(expected, mismatches)
    }

    @JvmStatic
    @JvmOverloads
    fun requestMismatches(
      expected: IRequest,
      actual: IRequest,
      pluginConfiguration: Map<String, PluginConfiguration> = mapOf()
    ): RequestMatchResult {
      logger.debug { "comparing to expected request: \n$expected" }
      logger.debug { "pluginConfiguration=$pluginConfiguration" }

      val pathContext = MatchingContext(expected.matchingRules.rulesForCategory("path"), false, pluginConfiguration)
      val bodyContext = MatchingContext(expected.matchingRules.rulesForCategory("body"), false, pluginConfiguration)
      val queryContext = MatchingContext(expected.matchingRules.rulesForCategory("query"), false, pluginConfiguration, true)
      val headerContext = MatchingContext(expected.matchingRules.rulesForCategory("header"), false, pluginConfiguration, true)

      return RequestMatchResult(Matching.matchMethod(expected.method, actual.method),
        Matching.matchPath(expected, actual, pathContext),
        Matching.matchQuery(expected, actual, queryContext),
        Matching.matchCookies(expected.cookies(), actual.cookies(), headerContext),
        Matching.matchRequestHeaders(expected, actual, headerContext),
        Matching.matchBody(expected.asHttpPart(), actual.asHttpPart(), bodyContext))
    }
  }
}
