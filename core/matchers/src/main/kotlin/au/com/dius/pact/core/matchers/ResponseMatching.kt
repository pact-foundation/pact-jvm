package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.matchers.engine.MatchingConfiguration
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.matchers.engine.V2MatchingEngine
import au.com.dius.pact.core.matchers.engine.V2MatchingEngine.v2EngineEnabled
import au.com.dius.pact.core.matchers.engine.resolvers.HttpResponseValueResolver
import au.com.dius.pact.core.model.IResponse
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.SynchronousRequestResponse
import io.github.oshai.kotlinlogging.KLogging
import io.pact.plugins.jvm.core.PluginConfiguration

sealed class ResponseMatch
object FullResponseMatch : ResponseMatch()
data class ResponseMismatch(val mismatches: List<Mismatch>) : ResponseMatch()

object ResponseMatching : KLogging() {

  @JvmStatic
  fun matchRules(
    pact: Pact,
    interaction: SynchronousRequestResponse,
    actual: IResponse,
    pluginConfiguration: Map<String, PluginConfiguration> = mapOf()
  ): ResponseMatch {
    val mismatches = responseMismatches(pact, interaction, actual, pluginConfiguration)
    return if (mismatches.isEmpty()) FullResponseMatch
    else ResponseMismatch(mismatches)
  }

  @JvmStatic
  @JvmOverloads
  fun responseMismatches(
    pact: Pact,
    interaction: SynchronousRequestResponse,
    actual: IResponse,
    pluginConfiguration: Map<String, PluginConfiguration> = mapOf()
  ): List<Mismatch> {
    val expected = interaction.response

    if (v2EngineEnabled()) {
      val config = MatchingConfiguration.fromEnv()
      val context = PlanMatchingContext(pact.asV4Pact().unwrap(), interaction.asV4Interaction(), config)

      val plan = V2MatchingEngine.buildResponsePlan(expected, context)
      val executedPlan = plan.execute(context, HttpResponseValueResolver(actual))

      if (config.logPlanSummary) {
        logger.info { executedPlan.generateSummary(config.colouredOutput) }
      }

      val result = executedPlan.intoResponseMatchResult()
      val statusMismatch = if (result.status != null) {
        listOf(StatusMismatch(expected.status, actual.status))
      } else emptyList()
      return statusMismatch + result.headers.flatMap { it.result } + result.body.mismatches
    }

    val statusContext = MatchingContext(expected.matchingRules.rulesForCategory("status"),
      true, pluginConfiguration)
    val bodyContext = MatchingContext(expected.matchingRules.rulesForCategory("body"),
      true, pluginConfiguration)
    val headerContext = MatchingContext(expected.matchingRules.rulesForCategory("header"),
      true, pluginConfiguration, true)

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
