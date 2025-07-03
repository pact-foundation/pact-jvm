package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.Into
import au.com.dius.pact.core.support.json.JsonValue
import com.github.ajalt.mordant.TermColors
import org.apache.commons.text.StringEscapeUtils.escapeJson

/** Terminator for tree transversal */
enum class Terminator {
  /** No termination */
  ALL,
  /** Terminate at containers */
  CONTAINERS
}

//  /// Pushes the node onto the front of the list
//  pub fn push_node(&mut self, node: ExecutionPlanNode) {
//    self.children.insert(0, node.into());
//  }

/**
 * Node in an executable plan tree
 */
data class ExecutionPlanNode(
  /** Type of the node */
  val nodeType: PlanNodeType,
  /** Any result associated with the node */
  val result: NodeResult?,
  /** Child nodes */
  val children: MutableList<ExecutionPlanNode> = mutableListOf()
): Into<ExecutionPlanNode> {

  /** Adds the node as a child */
  fun <N> add(node: N): ExecutionPlanNode where N: Into<ExecutionPlanNode> {
    children.add(node.into())
    return this
  }

  /** If the node is a leaf node */
  fun isEmpty(): Boolean {
    return when (nodeType) {
      is PlanNodeType.EMPTY -> true
      else -> children.isEmpty()
    }
  }

  /** If the node is not a leaf node */
  fun isNotEmpty(): Boolean {
    return when (nodeType) {
      is PlanNodeType.EMPTY -> false
      else -> children.isNotEmpty()
    }
  }

  override fun into() = this

  /** Returns the serialised text form of the node */
  @Suppress("LongMethod", "CyclomaticComplexMethod")
  fun strForm(buffer: StringBuilder): String {
    buffer.append('(')

    when (val n = nodeType) {
      is PlanNodeType.EMPTY -> {}
      is PlanNodeType.CONTAINER -> {
        buffer.append(':')
        if (n.label.any { ch -> ch.isWhitespace() }) {
          buffer.append('"')
          buffer.append(n.label)
          buffer.append('"')
        } else {
          buffer.append(n.label)
        }
        buffer.append('(')
        strFormChildren(buffer)
        buffer.append(')')

        if (result != null) {
          buffer.append("=>")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.ACTION -> {
        buffer.append('%')
        buffer.append(n.action)
        buffer.append('(')
        strFormChildren(buffer)
        buffer.append(')')

        if (result != null) {
          buffer.append("=>")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.VALUE -> {
        buffer.append(n.value.strForm())

        if (result != null) {
          buffer.append("=>")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.RESOLVE -> {
        buffer.append(n.path.toString())

        if (result != null) {
          buffer.append("=>")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.PIPELINE -> {
        buffer.append("->")
        buffer.append('(')
        strFormChildren(buffer)
        buffer.append(')')

        if (result != null) {
          buffer.append("=>")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.RESOLVE_CURRENT -> {
        buffer.append("~>")
        buffer.append(n.path.toString())

        if (result != null) {
          buffer.append("=>")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.SPLAT -> {
        buffer.append("**")
        buffer.append('(')
        strFormChildren(buffer)
        buffer.append(')')

        if (result != null) {
          buffer.append("=>")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.ANNOTATION -> {
        buffer.append("#{");
        buffer.append(escape(n.label))
        buffer.append('}')
      }
    }

    buffer.append(')')
    return buffer.toString()
  }

  private fun strFormChildren(buffer: StringBuilder) {
    val i = children.iterator()
    if (i.hasNext()) {
      buffer.append(i.next().strForm(buffer))
      while (i.hasNext()) {
        buffer.append(',')
        buffer.append(i.next().strForm(buffer))
      }
    }
  }

  /** Returns the human-readable text from of the node */
  @Suppress("LongMethod", "CyclomaticComplexMethod")
  fun prettyForm(buffer: StringBuilder, indent: Int) {
    val pad = " ".repeat(indent)

    when(val n = nodeType) {
      is PlanNodeType.EMPTY -> {}
      is PlanNodeType.CONTAINER -> {
        buffer.append(pad)
        buffer.append(':')
        if (n.label.any { ch -> ch.isWhitespace() }) {
          buffer.append('"')
          buffer.append(n.label)
          buffer.append('"')
        } else {
          buffer.append(n.label)
        }
        if (isEmpty()) {
          buffer.append(" ()")
        } else {
          buffer.append(" (\n")
          prettyFormChildren(buffer, indent)
          buffer.append(pad)
          buffer.append(')')
        }

        if (result != null) {
          buffer.append(" => ")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.ACTION -> {
        buffer.append(pad)
        buffer.append('%')
        buffer.append(n.action)
        if (isEmpty()) {
          buffer.append(" ()")
        } else {
          buffer.append(" (\n")
          prettyFormChildren(buffer, indent)
          buffer.append(pad)
          buffer.append(')')
        }

        if (result != null) {
          buffer.append(" => ")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.VALUE -> {
        buffer.append(pad)
        buffer.append(n.value.strForm());

        if (result != null) {
          buffer.append(" => ")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.RESOLVE -> {
        buffer.append(pad)
        buffer.append(n.path.toString())

        if (result != null) {
          buffer.append(" => ")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.PIPELINE -> {
        buffer.append(pad)
        buffer.append("->")
        if (isEmpty()) {
          buffer.append(" ()")
        } else {
          buffer.append(" (\n")
          prettyFormChildren(buffer, indent)
          buffer.append(pad)
          buffer.append(')')
        }

        if (result != null) {
          buffer.append(" => ")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.RESOLVE_CURRENT -> {
        buffer.append(pad)
        buffer.append("~>")
        buffer.append(n.path.toString())

        if (result != null) {
          buffer.append(" => ")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.SPLAT -> {
        buffer.append(pad)
        buffer.append("**")
        if (isEmpty()) {
          buffer.append(" ()")
        } else {
          buffer.append(" (\n")
          prettyFormChildren(buffer, indent)
          buffer.append(pad)
          buffer.append(')')
        }

        if (result != null) {
          buffer.append(" => ")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.ANNOTATION -> {
        buffer.append(pad)
        buffer.append("#{")
        buffer.append(escape(n.label))
        buffer.append('}')
      }
    }
  }

  private fun prettyFormChildren(buffer: StringBuilder, indent: Int) {
    val i = children.iterator()
    if (i.hasNext()) {
      i.next().prettyForm(buffer, indent + 2)
      while (i.hasNext()) {
        buffer.append(",\n")
        i.next().prettyForm(buffer, indent + 2)
      }
      buffer.append('\n')
    }
  }

  /** Return a summary of the execution to display in a console */
  fun generateSummary(ansiColor: Boolean, buffer: StringBuilder, indent: Int) {
    val pad = " ".repeat(indent)
    if (nodeType is PlanNodeType.CONTAINER) {
      buffer.append(pad)
      buffer.append(nodeType.label)
      buffer.append(':')

      val annotation = annotationNode()
      if (annotation != null) {
        buffer.append(' ')
        buffer.append(annotation)
      }

      if (result != null) {
        val t = TermColors()
        if (isLeafNode() || isTerminalContainer()) {
          if (result.isTruthy()) {
            if (ansiColor) {
              buffer.append(" - ${t.green("OK")}")
            } else {
              buffer.append(" - OK")
            }
          } else {
            val errors = childErrors(Terminator.ALL)
            if (result is NodeResult.ERROR) {
              if (ansiColor) {
                buffer.append(" - ${t.red("ERROR")} ${t.red(result.message)}")
              } else {
                buffer.append(" - ERROR ${result.message}")
              }
              val errorPad = " ".repeat(indent + nodeType.label.length + 2)
              for (error in errors) {
                buffer.append('\n')
                buffer.append(errorPad)
                if (ansiColor) {
                  buffer.append(" - ${t.red("ERROR")} ${t.red(error)}")
                } else {
                  buffer.append(" - ERROR $error")
                }
              }
            } else if (errors.size == 1) {
              if (ansiColor) {
                buffer.append(" - ${t.red("ERROR")} ${t.red(errors[0])}")
              } else {
                buffer.append(" - ERROR ${errors[0]}")
              }
            } else if (errors.isEmpty()) {
              if (ansiColor) {
                buffer.append(" - ${t.red("FAILED")}")
              } else {
                buffer.append(" - FAILED")
              }
            } else {
              val errorPad = " ".repeat(indent + nodeType.label.length + 2)
              for (error in errors) {
                buffer.append('\n')
                buffer.append(errorPad)
                if (ansiColor) {
                  buffer.append(" - ${t.red("ERROR")} ${t.red(error)}")
                } else {
                  buffer.append(" - ERROR $error")
                }
              }
            }
          }
        } else {
          val errors = childErrors(Terminator.CONTAINERS)
          if (result is NodeResult.ERROR) {
            if (ansiColor) {
              buffer.append(" - ${t.red("ERROR")} ${t.red(result.message)}")
            } else {
              buffer.append(" - ERROR ${result.message}")
            }
            val errorPad = " ".repeat(indent + nodeType.label.length + 2)
            for (error in errors) {
              buffer.append('\n')
              buffer.append(errorPad)
              if (ansiColor) {
                buffer.append(" - ${t.red("ERROR")} ${t.red(error)}")
              } else {
                buffer.append(" - ERROR $error")
              }
            }
          } else if (errors.size == 1) {
            if (ansiColor) {
              buffer.append(" - ${t.red("ERROR")} ${t.red(errors[0])}")
            } else {
              buffer.append(" - ERROR ${errors[0]}")
            }
          } else if (errors.isNotEmpty()) {
            val errorPad = " ".repeat(indent + nodeType.label.length + 2)
            for (error in errors) {
              buffer.append('\n')
              buffer.append(errorPad)
              if (ansiColor) {
                buffer.append(" - ${t.red("ERROR")} ${t.red(error)}")
              } else {
                buffer.append(" - ERROR $error")
              }
            }
          }
        }
      }

      buffer.append('\n')
      generateChildrenSummary(ansiColor, buffer, indent + 2)
    } else {
      generateChildrenSummary(ansiColor, buffer, indent)
    }
  }

  fun generateChildrenSummary(ansiColor: Boolean, buffer: StringBuilder, indent: Int) {
    for (child in children) {
      child.generateSummary(ansiColor, buffer, indent)
    }
  }

  /** Walks the tree to return any node that matches the given path */
  fun fetchNode(path: List<String>): ExecutionPlanNode? {
    return if (path.isEmpty()) {
      null
    } else if (matches(path[0])) {
      if (path.size > 1) {
        children.firstNotNullOfOrNull { child -> child.fetchNode(path.drop(1)) }
      } else {
        this.copy()
      }
    } else {
      null
    }
  }

  fun matches(identifier: String): Boolean {
    return when(val n = nodeType) {
      is PlanNodeType.EMPTY -> false
      is PlanNodeType.CONTAINER -> String.format(":%s", n.label) == identifier
      is PlanNodeType.ACTION -> String.format("%%%s", n.action) == identifier
      is PlanNodeType.VALUE -> false
      is PlanNodeType.RESOLVE -> n.toString() == identifier
      is PlanNodeType.PIPELINE -> "->" == identifier
      is PlanNodeType.RESOLVE_CURRENT -> "~>${n.path}" == identifier
      is PlanNodeType.SPLAT -> "**" == identifier
      is PlanNodeType.ANNOTATION -> false
    }
  }

  /** If the node is a splat node */
  fun isSplat() = nodeType == PlanNodeType.SPLAT

  /** If this node is a container */
  fun isContainer() = nodeType is PlanNodeType.CONTAINER

  /** If this node has no children */
  fun isLeafNode() = children.isEmpty()

  /** If this node is a container and there are no more containers in the subtree */
  fun isTerminalContainer() = isContainer() && !hasChildContainers()

  /** If this node has any containers in the subtree */
  fun hasChildContainers(): Boolean = children.any {
    it.isContainer() || it.hasChildContainers()
  }

  /** Returns the value for the node */
  fun value() = this.result

  fun annotationNode(): String? {
    return children.firstNotNullOfOrNull {
      if (it.nodeType is PlanNodeType.ANNOTATION) {
        it.nodeType.label
      } else {
        null
      }
    }
  }

  /**
   * Returns all the errors from the child nodes, either terminating at any child containers
   * or just returning all errors.
   */

  fun childErrors(terminator: Terminator): List<String> {
    val errors = mutableListOf<String>()
    for (child in children) {
      if (child.isContainer() && terminator == Terminator.ALL || !child.isContainer()) {
        if (child.result is NodeResult.ERROR) {
          errors.add(child.result.message)
        }
        errors.addAll(child.childErrors(terminator))
      }
    }
    return errors
  }

  /** Returns the first error found from this node stopping at child containers */
  fun error(): String? {
    return if (result is NodeResult.ERROR) {
      result.message
    } else {
      childErrors(Terminator.CONTAINERS).firstOrNull()
    }
  }

  /** Returns all the errors found from this node */
  fun errors(): List<String> {
    val errors = mutableListOf<String>()
    if (result is NodeResult.ERROR) {
      errors.add(result.message)
    }
    errors.addAll(childErrors(Terminator.ALL))
    return errors
  }

  /**
   * This a fold operation over the depth-first transversal of the containers in the tree
   * from this node.
   */
  fun <ACC> traverseContainers(
    initial: ACC,
    callback: (ACC, String, ExecutionPlanNode) -> ACC
  ): ACC {
    var accValue = initial

    for (child in children) {
      if (child.nodeType is PlanNodeType.CONTAINER) {
        accValue = callback(accValue, child.nodeType.label, child)
      }
      accValue = child.traverseContainers(accValue, callback)
    }

    return accValue
  }

  companion object {
    /** Constructor for a container node */
    fun container(label: String): ExecutionPlanNode {
      return ExecutionPlanNode(
        PlanNodeType.CONTAINER(label),
        null,
        mutableListOf()
      )
    }

    /** Constructor for an action node */
    fun action(value: String): ExecutionPlanNode {
      return ExecutionPlanNode(
        PlanNodeType.ACTION(value),
        null,
        mutableListOf()
      )
    }

    /** Constructor for a value node */
    fun <T> valueNode(value: T): ExecutionPlanNode where T: Into<NodeValue> {
      return ExecutionPlanNode(
        PlanNodeType.VALUE(value.into()),
        null,
        mutableListOf()
      )
    }

    /** Constructor for a value node */
    fun valueNode(value: String) = valueNode(Into { NodeValue.STRING(value) })
    /** Constructor for a value node */
    fun valueNode(value: UInt) = valueNode(Into { NodeValue.UINT(value) })
    /** Constructor for a value node */
    fun valueNode(value: JsonValue) = valueNode(Into { NodeValue.JSON(value) })

    /** Constructor for a resolve node */
    fun <T> resolveValue(resolveStr: T): ExecutionPlanNode where T: Into<DocPath> {
      return ExecutionPlanNode(
        PlanNodeType.RESOLVE(resolveStr.into()),
        null,
        mutableListOf()
      )
    }

    /** Constructor for a resolve current node */
    fun <T> resolveCurrentValue(resolveStr: T): ExecutionPlanNode where T: Into<DocPath> {
      return ExecutionPlanNode(
        PlanNodeType.RESOLVE_CURRENT(resolveStr.into()),
        null,
        mutableListOf()
      )
    }

//  /// Constructor for an apply node
//  pub fn apply() -> ExecutionPlanNode {
//    ExecutionPlanNode {
//      node_type: PlanNodeType::PIPELINE,
//      result: None,
//      children: vec![],
//    }
//  }

    /** Constructor for the splat node */
    fun splat(): ExecutionPlanNode {
      return ExecutionPlanNode(
        PlanNodeType.SPLAT,
        null,
        mutableListOf()
      )
    }

    fun <S>  annotation(description: S): ExecutionPlanNode where S: Into<String> {
      return ExecutionPlanNode(
        PlanNodeType.ANNOTATION(description.into()),
        null,
        mutableListOf()
      )
    }

    @JvmStatic
    fun escape(value: String): String {
      var qoutes = false
      var doubleQuotes = false
      for (ch in value) {
        if (ch == '\'') {
          doubleQuotes = true
          break
        } else if (!qoutes && ch.isWhitespace()) {
          qoutes = true
        }
      }

      return if (value.isEmpty()) {
        value
      } else if (doubleQuotes) {
        "\"${escapeJson(value)}\""
      } else if (qoutes) {
        "'$value'"
      } else {
        escapeJson(value)
      }
    }
  }
}
