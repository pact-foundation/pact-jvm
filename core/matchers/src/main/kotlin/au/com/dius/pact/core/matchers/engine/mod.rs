
/// Terminator for tree transversal
#[derive(Copy, Clone, Debug, PartialEq, PartialOrd)]
pub enum Terminator {
  /// No termination
  ALL,
  /// Terminate at containers
  CONTAINERS
}


/// Executes the request plan against the actual request.
pub fn execute_request_plan(
  plan: &ExecutionPlan,
  actual: &HttpRequest,
  context: &PlanMatchingContext
) -> anyhow::Result<ExecutionPlan> {
  let value_resolver = HttpRequestValueResolver {
    request: actual.clone()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(context);
  let path = vec![];
  let executed_tree = interpreter.walk_tree(&path, &plan.plan_root, &value_resolver)?;
  Ok(ExecutionPlan {
    plan_root: executed_tree
  })
}

