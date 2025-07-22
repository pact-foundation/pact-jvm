package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.matchers.MatchingContext
import au.com.dius.pact.core.matchers.MatchingContext.BestMatcherResult
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.support.Utils.lookupEnvironmentValue

/** Configuration for driving behaviour of the execution */
data class MatchingConfiguration @JvmOverloads constructor(
  /** If extra keys/values are allowed (and ignored) */
  val allowUnexpectedEntries: Boolean = false,
  /** If the executed plan should be logged */
  val logExecutedPlan: Boolean = false,
  /** If the executed plan summary should be logged */
  val logPlanSummary: Boolean = true,
  /** If output should be coloured */
  val colouredOutput: Boolean = true,
  /** If the raw plan should be logged (before it is executed) */
  val logRawPlan: Boolean = false
) {
  companion object {
    /**
     * Loads the matching engine configuration from system properties or environment variables:
     * `pact.matching.v2.logExecutedPlan` or `PACT_V2_MATCHING_LOG_EXECUTED_PLAN` - Enable to log the executed plan.
     * `pact.matching.v2.logRawPlan` or `PACT_V2_MATCHING_LOG_RAW_PLAN` - Enable to log the plan before it is executed.
     * `pact.matching.v2.logPlanSummary` or `PACT_V2_MATCHING_LOG_PLAN_SUMMARY` - Enable to log a summary of the
     * executed plan.
     * `pact.matching.v2.ColouredOutput` or `PACT_V2_MATCHING_COLOURED_OUTPUT` - Enables coloured output.
     */
    fun fromEnv(): MatchingConfiguration {
      var config = MatchingConfiguration()

      if (envVarSet("pact.matching.v2.logExecutedPlan") || envVarSet("PACT_V2_MATCHING_LOG_EXECUTED_PLAN")) {
        config = config.copy(logExecutedPlan = true)
      }

      if (envVarSet("pact.matching.v2.logRawPlan") || envVarSet("PACT_V2_MATCHING_LOG_RAW_PLAN")) {
        config = config.copy(logRawPlan = true)
      }

      if (envVarSet("pact.matching.v2.logPlanSummary") || envVarSet("PACT_V2_MATCHING_LOG_PLAN_SUMMARY")) {
        config = config.copy(logPlanSummary = true)
      }

      if (envVarSet("pact.matching.v2.ColouredOutput") || envVarSet("PACT_V2_MATCHING_COLOURED_OUTPUT")) {
        config = config.copy(colouredOutput = true)
      }

      return config
    }

    private fun envVarSet(key: String): Boolean {
      val value = lookupEnvironmentValue(key)?.lowercase()
      return value == "true" || value == "1"
    }
  }
}

/** Context to store data for use in executing an execution plan. */
open class PlanMatchingContext @JvmOverloads constructor(
  /** Pact the plan is for */
  val pact: V4Pact,
  /** Interaction that the plan id for */
  val interaction: V4Interaction,
  /** Configuration */
  val config: MatchingConfiguration,
  /** Matching rules to use */
  val matchingContext: MatchingContext = MatchingContext(MatchingRuleCategory(""), config.allowUnexpectedEntries)
) {

  /** If there is a matcher defined at the path in this context */
  fun matcherIsDefined(path: DocPath): Boolean {
    return when {
      path.firstField() == "headers" -> matchingContext.matcherDefined(path.asList().drop(2))
      else -> matchingContext.matcherDefined(path.asList())
    }
  }

  /** Select the best matcher to use for the given path */
  fun selectBestMatcher(path: DocPath): MatchingRuleGroup {
    return if (path.firstField() == "headers") {
      matchingContext.selectBestMatcher(path.asList().drop(2))
    } else {
      matchingContext.selectBestMatcher(path.asList())
    }
  }

  /**
   * Select the best matcher taking into account two paths
   */
  fun selectBestMatcher(path1: DocPath, path2: DocPath): MatchingRuleGroup {
    val result1 = matchingContext.matchers.matchingRules
      .map { BestMatcherResult(path = path1.asList(), pathExp = it.key, ruleGroup = it.value) }
      .filter { it.pathWeight > 0 }
    val result2 = matchingContext.matchers.matchingRules
      .map { BestMatcherResult(path = path2.asList(), pathExp = it.key, ruleGroup = it.value) }
      .filter { it.pathWeight > 0 }
    val result = (result1 + result2)
      .maxWithOrNull(compareBy<BestMatcherResult> { it.pathWeight }.thenBy { it.pathExp.length })
    return result?.ruleGroup?.copy(cascaded = result.pathTokens.size < path1.len()) ?: MatchingRuleGroup()
  }

  /**
   * If there is a type matcher defined at the path in this context
   */
  fun typeMatcherDefined(path: DocPath): Boolean {
    return matchingContext.typeMatcherDefined(path.asList())
  }

  /** Creates a clone of this context, but with the matching rules set for the Request Method */
  fun forMethod(): PlanMatchingContext {
    val httpInteraction = interaction.asSynchronousRequestResponse()
    val matchingRules = httpInteraction?.request?.matchingRules?.rulesForCategory("method")
      ?: MatchingRuleCategory("method")

    return PlanMatchingContext(
      pact,
      interaction,
      config,
      MatchingContext(matchingRules, config.allowUnexpectedEntries)
    )
  }

  /** Creates a clone of this context, but with the matching rules set for the Request Path */
  fun forPath(): PlanMatchingContext {
    val httpInteraction = interaction.asSynchronousRequestResponse()
    val matchingRules = httpInteraction?.request?.matchingRules?.rulesForCategory("path")
      ?: MatchingRuleCategory("path")

    return PlanMatchingContext(
      pact,
      interaction,
      config,
      MatchingContext(matchingRules, config.allowUnexpectedEntries)
    )
  }

  /** Creates a clone of this context, but with the matching rules set for the Request Query Parameters */
  fun forQuery(): PlanMatchingContext {
    val httpInteraction = interaction.asSynchronousRequestResponse()
    val matchingRules = httpInteraction?.request?.matchingRules?.rulesForCategory("query")
      ?: MatchingRuleCategory("query")

    return PlanMatchingContext(
      pact,
      interaction,
      config,
      MatchingContext(matchingRules, config.allowUnexpectedEntries, mapOf(), true)
    )
  }

  /** Creates a clone of this context, but with the matching rules set for the Request Headers */
  fun forHeaders(): PlanMatchingContext {
    val httpInteraction = interaction.asSynchronousRequestResponse()
    val matchingRules = httpInteraction?.request?.matchingRules?.rulesForCategory("header")
      ?: MatchingRuleCategory("header")

    return PlanMatchingContext(
      pact,
      interaction,
      config.copy(allowUnexpectedEntries = true),
      MatchingContext(matchingRules, config.allowUnexpectedEntries, mapOf(), true)
    )
  }

  /** Creates a clone of this context, but with the matching rules set for the Request Body */
  fun forBody(): PlanMatchingContext {
    val httpInteraction = interaction.asSynchronousRequestResponse()
    val matchingRules = httpInteraction?.request?.matchingRules?.rulesForCategory("body")
      ?: MatchingRuleCategory("body")

    return PlanMatchingContext(
      pact,
      interaction,
      config,
      MatchingContext(matchingRules, config.allowUnexpectedEntries)
    )
  }
}
