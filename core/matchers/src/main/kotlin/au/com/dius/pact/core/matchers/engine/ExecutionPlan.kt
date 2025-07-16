package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.matchers.BodyItemMatchResult
import au.com.dius.pact.core.matchers.BodyMatchResult
import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.BodyTypeMismatch
import au.com.dius.pact.core.matchers.HeaderMatchResult
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.matchers.MethodMismatch
import au.com.dius.pact.core.matchers.PathMismatch
import au.com.dius.pact.core.matchers.QueryMatchResult
import au.com.dius.pact.core.matchers.QueryMismatch
import au.com.dius.pact.core.matchers.RequestMatchResult
import au.com.dius.pact.core.support.isNotEmpty

/**
 * An executable plan that contains a tree of execution nodes
 */
open class ExecutionPlan(
  /** Root node for the plan tree */
  val planRoot: ExecutionPlanNode
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

  /**
   * Returns a standard Pact request match result for the executed plan
   */
  @Suppress("LongMethod", "CyclomaticComplexMethod")
  fun intoRequestMatchResult(): RequestMatchResult {
    val methodNode = fetchNode(listOf(":request", ":method"))
    val methodMismatch = if (methodNode != null) {
      val error = methodNode.error()
      if (error != null) {
        MethodMismatch("", "", error)
      } else null
    } else null

    val pathNode = fetchNode(listOf(":request", ":path"))
    val pathMismatch = if (pathNode != null) {
      val pathError = pathNode.error()
      if (pathError != null) {
        PathMismatch("", "", pathError)
      } else null
    } else null

    val queryNode = fetchNode(listOf(":request", ":query parameters"))
    val queryMismatches = if (queryNode != null) {
      val mismatches = queryNode.children.fold(mutableMapOf<String, List<QueryMismatch>>()) { acc, child ->
        if (child.nodeType is PlanNodeType.CONTAINER) {
          acc[child.nodeType.label] = child.errors()
            .map { QueryMismatch(child.nodeType.label, null, null, it) }
        } else {
          val mismatches = child.errors()
            .map { QueryMismatch("", null, null, it) }
          if (mismatches.isNotEmpty()) {
            acc[""] = mismatches
          }
        }
        acc
      }
      val errors = queryNode.childErrors(Terminator.CONTAINERS)
      if (errors.isNotEmpty()) {
        val additionalMismatches = errors
          .map { QueryMismatch("", null, null, it) }
        mismatches[""] = additionalMismatches
      }
      mismatches.map {
        QueryMatchResult(it.key, it.value)
      }
    } else emptyList()

    val headerNode = fetchNode(listOf(":request", ":headers"))
    val headerMismatches = if (headerNode != null) {
      val mismatches = headerNode.children.fold(mutableMapOf<String, List<HeaderMismatch>>()) { acc, child ->
        if (child.nodeType is PlanNodeType.CONTAINER) {
          acc[child.nodeType.label] = child.errors()
            .map { HeaderMismatch(child.nodeType.label, "", "", it) }
        } else {
          val mismatches = child.errors()
            .map { HeaderMismatch("", "", "", it) }
          if (mismatches.isNotEmpty()) {
            acc[""] = mismatches
          }
        }
        acc
      }
      val errors = headerNode.childErrors(Terminator.CONTAINERS)
      if (errors.isNotEmpty()) {
        val additionalMismatches = errors
          .map { HeaderMismatch("", "", "", it) }
        mismatches[""] = additionalMismatches
      }
      mismatches.map {
        HeaderMatchResult(it.key, it.value)
      }
    } else emptyList()

    val bodyNode = fetchNode(listOf(":request", ":body"))
    val bodyResult = if (bodyNode == null || bodyNode.result.orDefault().isTruthy()) {
      BodyMatchResult(null, emptyList())
    } else if (bodyNode.isEmpty()) {
      if (bodyNode.result is NodeResult.ERROR) {
        BodyMatchResult(null, listOf(BodyItemMatchResult("",
          listOf(BodyMismatch(null, null, bodyNode.result.message)))))
      } else {
        BodyMatchResult(null, emptyList())
      }
    } else {
      val firstError = bodyNode.error()
      if (firstError != null && firstError.lowercase().startsWith("body type error")) {
        BodyMatchResult(BodyTypeMismatch(null, null, firstError), emptyList())
      } else {
        val initial = mutableMapOf<String, List<BodyMismatch>>()
        val bodyMismatches = bodyNode.traverseContainers(initial) { acc, label, node ->
          val errors = node.childErrors(Terminator.CONTAINERS)
          if (errors.isNotEmpty()) {
            val mismatches = errors.map { BodyMismatch(null, null, it, label) }
            acc[label] = mismatches
          }
            acc
        }
        val results = bodyMismatches.map { BodyItemMatchResult(it.key, it.value) }.toMutableList()
        if (firstError.isNotEmpty()) {
          results.add(BodyItemMatchResult("", listOf(BodyMismatch(null, null, firstError!!))))
        }
        BodyMatchResult(null, results)
      }
    }

    return RequestMatchResult(
      method = methodMismatch,
      path = pathMismatch,
      query = queryMismatches,
      cookie = null,
      headers = headerMismatches,
      body = bodyResult
    )
  }
}
