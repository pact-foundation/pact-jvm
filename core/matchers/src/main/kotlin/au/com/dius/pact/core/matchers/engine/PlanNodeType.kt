package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.model.DocPath

/**
 * Enum for the type of Plan Node
 */
sealed class PlanNodeType {
  /** Default plan node is empty */
  data object EMPTY: PlanNodeType()
  /** Container node with a label */
  data class CONTAINER(val label: String): PlanNodeType()
  /** Action node with a function reference */
  data class ACTION(val action: String): PlanNodeType()
  /** Leaf node that contains a value */
  data class VALUE(val value: NodeValue): PlanNodeType()
  /** Leaf node that stores an expression to resolve against the test context */
  data class RESOLVE(val path: DocPath): PlanNodeType()
  /** Pipeline node (apply), which applies each node to the next as a pipeline returning the last */
  data object PIPELINE: PlanNodeType()
  /** Leaf node that stores an expression to resolve against the current stack item */
  data class RESOLVE_CURRENT(val path: DocPath): PlanNodeType()
  /** Splat node, which executes its children and then replaces itself with the result */
  data object SPLAT: PlanNodeType()
  /** Annotation node to help with the description of the plan. Not executable. */
  data class ANNOTATION(val label: String): PlanNodeType()
}
