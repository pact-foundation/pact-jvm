package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.support.Result

interface MatchingEngine {
  /**
   * Constructs an execution plan for the HTTP request part.
   */
  fun buildRequestPlan(expectedRequest: HttpRequest, context: PlanMatchingContext): Result<ExecutionPlan, Exception>
}

object V2MatchingEngine: MatchingEngine {
  // /// Constructs an execution plan for the HTTP request part.
  //pub fn build_request_plan(
  //  expected: &HttpRequest,
  //  context: &PlanMatchingContext
  //) -> anyhow::Result<ExecutionPlan> {
  //  let mut plan = ExecutionPlan::new("request");
  //
  //  plan.add(setup_method_plan(expected, &context.for_method())?);
  //  plan.add(setup_path_plan(expected, &context.for_path())?);
  //  plan.add(setup_query_plan(expected, &context.for_query())?);
  //  plan.add(setup_header_plan(expected, &context.for_headers())?);
  //  plan.add(setup_body_plan(expected, &context.for_body())?);
  //
  //  Ok(plan)
  //}
  override fun buildRequestPlan(
    expectedRequest: HttpRequest,
    context: PlanMatchingContext
  ): Result<ExecutionPlan, Exception> {
    TODO("Not yet implemented")
  }
}
