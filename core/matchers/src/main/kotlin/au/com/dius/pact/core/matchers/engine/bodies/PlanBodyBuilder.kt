package au.com.dius.pact.core.matchers.engine.bodies

import au.com.dius.pact.core.matchers.engine.ExecutionPlanNode
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.model.ContentType

/**
 * Interface for implementations of builders for different types of bodies
 */
interface PlanBodyBuilder {
  /** If this builder supports a namespace for nodes. */
  fun namespace(): String? = null

  /** If this builder supports the given content type */
  fun supportsType(contentType: ContentType): Boolean

  /** Build the plan for the expected body */
  fun buildPlan(content: ByteArray, context: PlanMatchingContext): ExecutionPlanNode
}

private val BODY_PLAN_BUILDERS: List<PlanBodyBuilder> = listOf(
  JsonPlanBuilder,
  // XMLPlanBuilder(),
)

fun getBodyPlanBuilder(contentType: ContentType): PlanBodyBuilder? {
  return BODY_PLAN_BUILDERS.find { it.supportsType(contentType) }
}
