package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.IResponse
import io.pact.plugins.jvm.core.PluginConfiguration
import mu.KLogging

sealed class ResponseMatch
object FullResponseMatch : ResponseMatch()
data class ResponseMismatch(val mismatches: List<Mismatch>) : ResponseMatch()

object ResponseMatching : KLogging() {

  @JvmStatic
  fun matchRules(
    expected: IResponse,
    actual: IResponse,
    pluginConfiguration: Map<String, PluginConfiguration> = mapOf()
  ): ResponseMatch {
    val mismatches = responseMismatches(expected, actual, pluginConfiguration)
    return if (mismatches.isEmpty()) FullResponseMatch
    else ResponseMismatch(mismatches)
  }

  @JvmStatic
  @JvmOverloads
  fun responseMismatches(
    expected: IResponse,
    actual: IResponse,
    pluginConfiguration: Map<String, PluginConfiguration> = mapOf()
  ): List<Mismatch> {
    val statusContext = MatchingContext(expected.matchingRules.rulesForCategory("status"), true, pluginConfiguration)
    val bodyContext = MatchingContext(expected.matchingRules.rulesForCategory("body"), true, pluginConfiguration)
    val headerContext = MatchingContext(expected.matchingRules.rulesForCategory("header"), true, pluginConfiguration)

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
