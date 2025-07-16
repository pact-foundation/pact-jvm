package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.matchers.MatchingContext
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup

/** Configuration for driving behaviour of the execution */
data class MatchingConfiguration(
  /** If extra keys/values are allowed (and ignored) */
  val allowUnexpectedEntries: Boolean = false,
  /** If the executed plan should be logged */
  val logExecutedPlan: Boolean = false,
  /** If the executed plan summary should be logged */
  val logPlanSummary: Boolean = true,
  /** If output should be coloured */
  val colouredOutput: Boolean = true
)

//impl MatchingConfiguration {
//  /// Configures the matching engine configuration from environment variables:
//  /// * `V2_MATCHING_LOG_EXECUTED_PLAN` - Enable to log the executed plan.
//  /// * `V2_MATCHING_LOG_PLAN_SUMMARY` - Enable to log a summary of the executed plan.
//  /// * `V2_MATCHING_COLOURED_OUTPUT` - Enables coloured output.
//  pub fn init_from_env() -> Self {
//    let mut config = MatchingConfiguration::default();
//
//    if let Some(val) = env_var_set("V2_MATCHING_LOG_EXECUTED_PLAN") {
//      config.log_executed_plan = val;
//    }
//    if let Some(val) = env_var_set("V2_MATCHING_LOG_PLAN_SUMMARY") {
//      config.log_plan_summary = val;
//    }
//    if let Some(val) = env_var_set("V2_MATCHING_COLOURED_OUTPUT") {
//      config.coloured_output = val;
//    }
//
//    config
//  }
//}
//
//fn env_var_set(name: &str) -> Option<bool> {
//  std::env::var(name)
//    .ok()
//    .map(|v| ["true", "1"].contains(&v.to_lowercase().as_str()))
//}

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
