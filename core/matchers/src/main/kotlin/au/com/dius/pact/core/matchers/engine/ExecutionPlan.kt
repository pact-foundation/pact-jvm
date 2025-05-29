package au.com.dius.pact.core.matchers.engine

/**
 * An executable plan that contains a tree of execution nodes
 */
open class ExecutionPlan(
  /** Root node for the plan tree */
  private val planRoot: ExecutionPlanNode
) {
  /** Creates a new empty execution plan with a single root container */
  constructor(label: String) : this(ExecutionPlanNode.container(label))

  /** Adds the node as the root node if the node is not empty (i.e. not a leaf node). */
  fun add(node: ExecutionPlanNode) {
    if (node.isNotEmpty()) {
      planRoot.add(node)
    }
  }

  /** Returns the serialised text form of the execution  plan. */
  fun strForm(): String {
    val buffer = StringBuilder()
    buffer.append('(')
    buffer.append(planRoot.strForm(buffer))
    buffer.append(')')
    return buffer.toString()
  }

  /** Returns the human-readable text form of the execution plan. */
  fun prettyForm(): String {
    val buffer = StringBuilder()
    buffer.append("(\n")
    planRoot.prettyForm(buffer, 2)
    buffer.append("\n)\n")
    return buffer.toString()
  }

  /** Return a summary of the execution to display in a console */
  fun generateSummary(ansiColor: Boolean): String {
    val buffer = StringBuilder()
    planRoot.generateSummary(ansiColor, buffer, 0)
    return buffer.toString()
  }

  /** Walks the tree to return any node that matches the given path */
  fun fetchNode(path: List<String>): ExecutionPlanNode? {
    return planRoot.fetchNode(path)
  }
}
