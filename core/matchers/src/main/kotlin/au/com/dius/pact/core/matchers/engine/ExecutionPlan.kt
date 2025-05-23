package au.com.dius.pact.core.matchers.engine

//pub struct ExecutionPlan {
//  /// Root node for the plan tree
//  pub plan_root: ExecutionPlanNode
//}
//
//impl ExecutionPlan {
//  /// Creates a new empty execution plan with a single root container
//  fn new(label: &str) -> ExecutionPlan {
//    ExecutionPlan {
//      plan_root: ExecutionPlanNode::container(label)
//    }
//  }
//
//  /// Adds the node as the root node if the node is not empty (i.e. not a leaf node).
//  pub fn add(&mut self, node: ExecutionPlanNode) {
//    if !node.is_empty() {
//      self.plan_root.add(node);
//    }
//  }
//
//  /// Returns the serialised text form of the execution  plan.
//  pub fn str_form(&self) -> String {
//    let mut buffer = String::new();
//    buffer.push('(');
//    buffer.push_str(self.plan_root.str_form().as_str());
//    buffer.push(')');
//    buffer
//  }
//
//  /// Returns the human-readable text form of the execution plan.
//  pub fn pretty_form(&self) -> String {
//    let mut buffer = String::new();
//    buffer.push_str("(\n");
//    self.plan_root.pretty_form(&mut buffer, 2);
//    buffer.push_str("\n)\n");
//    buffer
//  }
//
//  /// Return a summary of the execution to display in a console
//  pub fn generate_summary(&self, ansi_color: bool) -> String {
//    let mut buffer = String::new();
//    self.plan_root.generate_summary(ansi_color, &mut buffer, 0);
//    buffer
//  }
//
//  /// Walks the tree to return any node that matches the given path
//  pub fn fetch_node(&self, path: &[&str]) -> Option<ExecutionPlanNode> {
//    self.plan_root.fetch_node(path)
//  }
//}

/**
 * An executable plan that contains a tree of execution nodes
 */
open class ExecutionPlan {
}
