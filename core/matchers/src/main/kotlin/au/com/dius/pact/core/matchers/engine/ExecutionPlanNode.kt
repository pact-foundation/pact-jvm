package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.Into
import org.apache.commons.text.StringEscapeUtils.escapeJson

//  /// Clones this node, replacing the value with the given one
//  pub fn clone_with_value(&self, value: NodeValue) -> ExecutionPlanNode {
//    ExecutionPlanNode {
//      node_type: self.node_type.clone(),
//      result: Some(NodeResult::VALUE(value)),
//      children: self.children.clone()
//    }
//  }
//
//  /// Clones this node, replacing the result with the given one
//  pub fn clone_with_result(&self, result: NodeResult) -> ExecutionPlanNode {
//    ExecutionPlanNode {
//      node_type: self.node_type.clone(),
//      result: Some(result),
//      children: self.children.clone()
//    }
//  }
//
//  /// Clones this node, replacing the result with the given one
//  pub fn clone_with_children<I>(&self, children: I) -> ExecutionPlanNode
//    where I: IntoIterator<Item = ExecutionPlanNode> {
//    ExecutionPlanNode {
//      node_type: self.node_type.clone(),
//      result: self.result.clone(),
//      children: children.into_iter().collect()
//    }
//  }
//
//  /// Pushes the node onto the front of the list
//  pub fn push_node(&mut self, node: ExecutionPlanNode) {
//    self.children.insert(0, node.into());
//  }
//
//  /// If the node is a splat node
//  pub fn is_splat(&self) -> bool {
//    match self.node_type {
//      PlanNodeType::SPLAT => true,
//      _ => false
//    }
//  }
//
//  /// Returns the value for the node
//  pub fn value(&self) -> Option<NodeResult> {
//    self.result.clone()
//  }
//
//  fn annotation_node(&self) -> Option<String> {
//    self.children.iter().find_map(|child| {
//      if let PlanNodeType::ANNOTATION(annotation) = &child.node_type {
//        Some(annotation.clone())
//      } else {
//        None
//      }
//    })
//  }
//
//  /// If this node has no children
//  pub fn is_leaf_node(&self) -> bool {
//    self.children.is_empty()
//  }
//
//  /// If this node is a container and there are no more containers in the subtree
//  pub fn is_terminal_container(&self) -> bool {
//    self.is_container() && !self.has_child_containers()
//  }
//
//  /// If this node is a container
//  pub fn is_container(&self) -> bool {
//    if let PlanNodeType::CONTAINER(_) = &self.node_type {
//      true
//    } else {
//      false
//    }
//  }
//
//  /// If this node has any containers in the subtree
//  pub fn has_child_containers(&self) -> bool {
//    self.children.iter().any(|child| {
//      if let PlanNodeType::CONTAINER(_) = &child.node_type {
//        true
//      } else {
//        child.has_child_containers()
//      }
//    })
//  }
//
//  /// Returns all the errors from the child nodes, either terminating at any child containers
//  /// ot just returning all errors.
//  pub fn child_errors(&self, terminator: Terminator) -> Vec<String> {
//    let mut errors = vec![];
//    for child in &self.children {
//      if child.is_container() && terminator == Terminator::ALL || !child.is_container() {
//        if let Some(NodeResult::ERROR(error)) = &child.result {
//          errors.push(error.clone());
//        }
//        errors.extend_from_slice(child.child_errors(terminator).as_slice());
//      }
//    }
//    errors
//  }
//
//  /// Returns the first error found from this node stopping at child containers
//  pub fn error(&self) -> Option<String> {
//    if let Some(NodeResult::ERROR(err)) = &self.result {
//      Some(err.clone())
//    } else {
//      self.child_errors(Terminator::CONTAINERS).first().cloned()
//    }
//  }
//
//  /// Returns all the errors found from this node
//  pub fn errors(&self) -> Vec<String> {
//    let mut errors = vec![];
//    if let Some(NodeResult::ERROR(err)) = &self.result {
//      errors.push(err.clone());
//    }
//    errors.extend_from_slice(&self.child_errors(Terminator::ALL));
//    errors
//  }
//
//  /// This a fold operation over the depth-first transversal of the containers in the tree
//  /// from this node.
//  pub fn traverse_containers<ACC, F>(&self, acc: ACC, mut callback: F) -> ACC
//    where F: FnMut(ACC, String, &ExecutionPlanNode) -> ACC + Clone,
//          ACC: Default
//  {
//    let acc_cell = Cell::new(acc);
//    for child in &self.children {
//      if let PlanNodeType::CONTAINER(label) = &child.node_type {
//        let acc = acc_cell.take();
//        let result = callback(acc, label.clone(), child);
//        acc_cell.set(result);
//      }
//      let acc = acc_cell.take();
//      let result = child.traverse_containers(acc, callback.clone());
//      acc_cell.set(result);
//    }
//    acc_cell.into_inner()
//  }
//}
//
//impl From<&mut ExecutionPlanNode> for ExecutionPlanNode {
//  fn from(value: &mut ExecutionPlanNode) -> Self {
//    value.clone()
//  }
//}
//
//impl From<anyhow::Error> for ExecutionPlanNode {
//  fn from(value: anyhow::Error) -> Self {
//    ExecutionPlanNode {
//      result: Some(NodeResult::ERROR(value.to_string())),
//      .. ExecutionPlanNode::default()
//    }
//  }

/**
 * Node in an executable plan tree
 */
data class ExecutionPlanNode(
  /** Type of the node */
  val nodeType: PlanNodeType,
  /** Any result associated with the node */
  var result: NodeResult?,
  /** Child nodes */
  val children: MutableList<ExecutionPlanNode>
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
  @Suppress("LongMethod")
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
        buffer.append(n.value)
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
          buffer.append("=>")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.ACTION -> {
        buffer.append(pad)
        buffer.append('%')
        buffer.append(n.value)
        if (isEmpty()) {
          buffer.append(" ()")
        } else {
          buffer.append(" (\n")
          prettyFormChildren(buffer, indent)
          buffer.append(pad)
          buffer.append(')')
        }

        if (result != null) {
          buffer.append("=>")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.VALUE -> {
        buffer.append(pad)
        buffer.append(n.value.strForm());

        if (result != null) {
          buffer.append("=>")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.RESOLVE -> {
        buffer.append(pad)
        buffer.append(n.path.toString())

        if (result != null) {
          buffer.append("=>")
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
          buffer.append("=>")
          buffer.append(result.toString())
        }
      }
      is PlanNodeType.RESOLVE_CURRENT -> {
        buffer.append(pad)
        buffer.append("~>")
        buffer.append(n.path.toString())

        if (result != null) {
          buffer.append("=>")
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
          buffer.append("=>")
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
//    let pad = " ".repeat(indent);
//
//    match &self.node_type {
//      PlanNodeType::CONTAINER(label) => {
//        buffer.push_str(pad.as_str());
//        buffer.push_str(label.as_str());
//        buffer.push(':');
//
//        if let Some(annotation) = self.annotation_node() {
//          buffer.push(' ');
//          buffer.push_str(annotation.as_str());
//        }
//
//        if let Some(result) = &self.result {
//          if self.is_leaf_node() || self.is_terminal_container() {
//            if result.is_truthy() {
//              if ansi_color {
//                buffer.push_str(format!(" - {}", Green.paint("OK")).as_str());
//              } else {
//                buffer.push_str(" - OK");
//              }
//            } else {
//              let errors = self.child_errors(Terminator::ALL);
//              if let NodeResult::ERROR(err) = result {
//                if ansi_color {
//                  buffer.push_str(format!(" - {} {}", Red.paint("ERROR"), Red.paint(err)).as_str());
//                } else {
//                  buffer.push_str(format!(" - ERROR {}", err).as_str());
//                }
//                let error_pad = " ".repeat(indent + label.len() + 2);
//                for error in errors {
//                  buffer.push('\n');
//                  buffer.push_str(error_pad.as_str());
//                  if ansi_color {
//                    buffer.push_str(format!(" - {} {}", Red.paint("ERROR"), Red.paint(error)).as_str());
//                  } else {
//                    buffer.push_str(format!(" - ERROR {}", error).as_str());
//                  }
//                }
//              } else if errors.len() == 1 {
//                if ansi_color {
//                  buffer.push_str(format!(" - {} {}", Red.paint("ERROR"), Red.paint(errors[0].as_str())).as_str());
//                } else {
//                  buffer.push_str(format!(" - ERROR {}", errors[0]).as_str())
//                }
//              } else if errors.is_empty() {
//                if ansi_color {
//                  buffer.push_str(format!(" - {}", Red.paint("FAILED")).as_str());
//                } else {
//                  buffer.push_str(" - FAILED")
//                }
//              } else {
//                let error_pad = " ".repeat(indent + label.len() + 2);
//                for error in errors {
//                  buffer.push('\n');
//                  buffer.push_str(error_pad.as_str());
//                  if ansi_color {
//                    buffer.push_str(format!(" - {} {}", Red.paint("ERROR"), Red.paint(error)).as_str());
//                  } else {
//                    buffer.push_str(format!(" - ERROR {}", error).as_str());
//                  }
//                }
//              }
//            }
//          } else {
//            let errors = self.child_errors(Terminator::CONTAINERS);
//            if let NodeResult::ERROR(err) = result {
//              if ansi_color {
//                buffer.push_str(format!(" - {} {}", Red.paint("ERROR"), Red.paint(err)).as_str());
//              } else {
//                buffer.push_str(format!(" - ERROR {}", err).as_str());
//              }
//              let error_pad = " ".repeat(indent + label.len() + 2);
//              for error in errors {
//                buffer.push('\n');
//                buffer.push_str(error_pad.as_str());
//                if ansi_color {
//                  buffer.push_str(format!(" - {} {}", Red.paint("ERROR"), Red.paint(error)).as_str());
//                } else {
//                  buffer.push_str(format!(" - ERROR {}", error).as_str());
//                }
//              }
//            } else if errors.len() == 1 {
//              if ansi_color {
//                buffer.push_str(format!(" - {} {}", Red.paint("ERROR"), Red.paint(errors[0].as_str())).as_str());
//              } else {
//                buffer.push_str(format!(" - ERROR {}", errors[0]).as_str())
//              }
//            } else if !errors.is_empty() {
//              let error_pad = " ".repeat(indent + label.len() + 2);
//              for error in errors {
//                buffer.push('\n');
//                buffer.push_str(error_pad.as_str());
//                if ansi_color {
//                  buffer.push_str(format!(" - {} {}", Red.paint("ERROR"), Red.paint(error)).as_str());
//                } else {
//                  buffer.push_str(format!(" - ERROR {}", error).as_str());
//                }
//              }
//            }
//          }
//        }
//
//        buffer.push('\n');
//
//        self.generate_children_summary(ansi_color, buffer, indent + 2);
//      }
//      _ => self.generate_children_summary(ansi_color, buffer, indent)
//    }
  }

//  fn generate_children_summary(&self, ansi_color: bool, buffer: &mut String, indent: usize) {
//    for child in &self.children {
//      child.generate_summary(ansi_color, buffer, indent);
//    }
//  }
//

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
      is PlanNodeType.ACTION -> String.format("%%%s", n.value) == identifier
      is PlanNodeType.VALUE -> false
      is PlanNodeType.RESOLVE -> n.toString() == identifier
      is PlanNodeType.PIPELINE -> "->" == identifier
      is PlanNodeType.RESOLVE_CURRENT -> "~>${n.path}" == identifier
      is PlanNodeType.SPLAT -> "**" == identifier
      is PlanNodeType.ANNOTATION -> false
    }
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
//
//  /// Constructor for the splat node
//  pub fn splat() -> ExecutionPlanNode {
//    ExecutionPlanNode {
//      node_type: PlanNodeType::SPLAT,
//      result: None,
//      children: vec![]
//    }
//  }

    fun <S> annotation(description: S): ExecutionPlanNode where S: Into<String> {
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
