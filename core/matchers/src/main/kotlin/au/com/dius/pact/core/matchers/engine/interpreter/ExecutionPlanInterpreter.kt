package au.com.dius.pact.core.matchers.engine.interpreter

import au.com.dius.pact.core.matchers.engine.ExecutionPlanNode
import au.com.dius.pact.core.matchers.engine.NodeResult
import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.matchers.engine.PlanNodeType
import au.com.dius.pact.core.matchers.engine.resolvers.ValueResolver
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.JsonUtils
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.handleWith
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Main interpreter for the matching plan AST
 */
class ExecutionPlanInterpreter(
  /** Context to use to execute the plan */
  val context: PlanMatchingContext
) {
  /** Stack of intermediate values (used by the pipeline operator and apply action) */
  private val valueStack: MutableList<NodeResult?> = mutableListOf()

   /** Walks the tree from a given node, executing all visited nodes */
   @Suppress("LongMethod", "CyclomaticComplexMethod")
  fun walkTree(path: List<String>, node: ExecutionPlanNode, valueResolver: ValueResolver): ExecutionPlanNode {
    return when (node.nodeType) {
      is PlanNodeType.ACTION -> {
        logger.trace { "walk_tree ==> Action node, path = $path, action = ${node.nodeType.action}" }
        executeAction(node.nodeType.action, valueResolver, node, path)
      }

      is PlanNodeType.ANNOTATION -> {
        logger.trace { "walk_tree ==> Annotation node, path = $path" }
        node.copy()
      }

      is PlanNodeType.CONTAINER -> {
        logger.trace { "walk_tree ==> Container node, path = $path, label = ${node.nodeType.label}" }

        val result = mutableListOf<ExecutionPlanNode>()
        val childPath = path + node.nodeType.label
        var status: NodeResult = NodeResult.OK
        val loopItems = ArrayDeque(node.children)

        while (loopItems.isNotEmpty()) {
          val child = loopItems.removeFirst()
          val childResult = walkTree(childPath, child, valueResolver)
          status = status.and(childResult.result)
          result.add(childResult)
          if (childResult.isSplat()) {
            for (item in childResult.children.reversed()) {
              loopItems.addFirst(item)
            }
          }
        }

        ExecutionPlanNode(
          node.nodeType,
          status.truthy(),
          result
        )
      }

      PlanNodeType.EMPTY -> {
        logger.trace { "walk_tree ==> Empty node, path = $path" }
        node.copy()
      }

      PlanNodeType.PIPELINE -> {
        logger.trace { "walk_tree ==> Apply pipeline node" }

        val childPath = path
        val childResults = mutableListOf<ExecutionPlanNode>()
        val loopItems = ArrayDeque(node.children)
        pushResult(null)

        // TODO: Need a short circuit here if any child results in an error
        while (loopItems.isNotEmpty()) {
          val child = loopItems.removeFirst()
          val childResult = walkTree(childPath, child, valueResolver)
          updateResult(childResult.result)
          childResults.add(childResult)
          if (childResult.isSplat()) {
            for (item in childResult.children.reversed()) {
              loopItems.addFirst(item)
            }
          }
        }

        when (val result = popResult()) {
          null -> {
            logger.trace { "Value from stack is empty, path = $path" }
            ExecutionPlanNode(
              node.nodeType,
              NodeResult.ERROR("Value from stack is empty"),
              childResults
            )
          }
          else -> {
            ExecutionPlanNode(
              node.nodeType,
              result,
              childResults
            )
          }
        }
      }

      is PlanNodeType.RESOLVE -> {
        logger.trace { "walk_tree ==> Resolve node, path = $path, resolve_path = ${node.nodeType.path}" }
        when (val result = valueResolver.resolve(node.nodeType.path, context)) {
          is Result.Ok -> node.copy(result = NodeResult.VALUE(result.value))
          is Result.Err -> {
            logger.trace {
              "Resolve node failed, path = $path, resolve_path = ${node.nodeType.path}, error = ${result.error}"
            }
            node.copy(result = NodeResult.ERROR(result.error))
          }
        }
      }

      is PlanNodeType.RESOLVE_CURRENT -> {
        logger.trace { "walk_tree ==> Resolve current node, path = $path, expression = ${node.nodeType.path}" }
        when (val result = resolveStackValue(node.nodeType.path)) {
          is Result.Ok -> node.copy(result = NodeResult.VALUE(result.value))
          is Result.Err -> {
            logger.trace {
              "Resolve node failed, path = $path, expression = ${node.nodeType.path}, error = ${result.error}"
            }
            node.copy(result = NodeResult.ERROR(result.error))
          }
        }
      }

      PlanNodeType.SPLAT -> {
        logger.trace { "walk_tree ==> Resolve splat node, path = $path" }

        val childPath = path
        val childResults = mutableListOf<ExecutionPlanNode>()

        for (child in node.children) {
          val childResult = walkTree(childPath, child, valueResolver)
          when (val result = childResult.result) {
            is NodeResult.VALUE -> when (result.value) {
              is NodeValue.MMAP -> for (entry in result.value.entries) {
                val value = NodeValue.ENTRY(entry.key, NodeValue.SLIST(entry.value))
                childResults.add(childResult.copy(result = NodeResult.VALUE(value)))
              }
              is NodeValue.SLIST -> for (item in result.value.items) {
                childResults.add(childResult.copy(result = NodeResult.VALUE(NodeValue.STRING(item))))
              }
              else -> childResults.add(childResult)
            }
            else -> childResults.add(childResult)
          }
        }

        ExecutionPlanNode(
          node.nodeType,
          NodeResult.OK,
          childResults
        )
      }

      is PlanNodeType.VALUE -> {
        logger.trace { "walk_tree ==> Value node, path = $path, value = ${node.nodeType.value}" }
        val result = when (val v = node.nodeType.value) {
          is NodeValue.NAMESPACED -> {
            when (v.name) {
              "json" -> {
                when (val jsonResult = handleWith<JsonValue> { JsonParser.parseString(v.value) }) {
                  is Result.Ok -> NodeResult.VALUE(NodeValue.JSON(jsonResult.value))
                  is Result.Err -> NodeResult.ERROR(jsonResult.error.message!!)
                }
              }
              // #[cfg(feature = "xml")]
              // "xml" => kiss_xml::parse_str(unescape(value).unwrap_or_else(|_| value.clone()))
              //   .map(|doc| NodeValue::XML(XmlValue::Element(doc.root_element().clone())))
              //   .map_err(|err| anyhow!("Failed to parse XML value: {}", err)),
              else -> NodeResult.ERROR("'${v.name}' is not a known namespace")
            }
          }
          else -> NodeResult.VALUE(node.nodeType.value)
        }

        node.copy(result = result)
      }
    }
  }

  private fun executeAction(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    path: List<String>
  ): ExecutionPlanNode {
    logger.trace { "Executing action, action=$action" }

    val actionPath = path + action
    return if (action.startsWith("match:")) {
      when (val matcher = action.removePrefix("match:")) {
        "" -> node.copy(result = NodeResult.ERROR("'$action' is not a valid action"))
        else -> executeMatch(action, matcher, valueResolver, node, actionPath)
      }
    } else {
      when (action) {
        else -> node.copy(result = NodeResult.ERROR("'$action' is not a valid action"))
      }
    //      match action {
    //        "upper-case" => self.execute_change_case(action, value_resolver, node, &action_path, true),
    //        "lower-case" => self.execute_change_case(action, value_resolver, node, &action_path, false),
    //        "to-string" => self.execute_to_string(action, value_resolver, node, &action_path),
    //        "length" => self.execute_length(action, value_resolver, node, &action_path),
    //        "expect:empty" => self.execute_expect_empty(action, value_resolver, node, &action_path),
    //        "convert:UTF8" => self.execute_convert_utf8(action, value_resolver, node, &action_path),
    //        "if" => self.execute_if(value_resolver, node, &action_path),
    //        "and" => self.execute_and(value_resolver, node, &action_path),
    //        "or" => self.execute_or(value_resolver, node, &action_path),
    //        "tee" => self.execute_tee(value_resolver, node, &action_path),
    //        "apply" => self.execute_apply(node),
    //        "push" => self.execute_push(node),
    //        "pop" => self.execute_pop(node),
    //        "json:parse" => self.execute_json_parse(action, value_resolver, node, &action_path),
    //        #[cfg(feature = "xml")]
    //        "xml:parse" => self.execute_xml_parse(action, value_resolver, node, &action_path),
    //        #[cfg(feature = "xml")]
    //        "xml:tag-name" => self.execute_xml_tag_name(action, value_resolver, node, &action_path),
    //        #[cfg(feature = "xml")]
    //        "xml:value" => self.execute_xml_value(action, value_resolver, node, &action_path),
    //        #[cfg(feature = "xml")]
    //        "xml:attributes" => self.execute_xml_attributes(action, value_resolver, node, &action_path),
    //        "json:expect:empty" => self.execute_json_expect_empty(action, value_resolver, node, &action_path),
    //        "json:match:length" => self.execute_json_match_length(action, value_resolver, node, &action_path),
    //        "json:expect:entries" => self.execute_json_expect_entries(action, value_resolver, node, &action_path),
    //        "check:exists" => self.execute_check_exists(action, value_resolver, node, &action_path),
    //        "expect:entries" => self.execute_check_entries(action, value_resolver, node, &action_path),
    //        "expect:only-entries" => self.execute_check_entries(action, value_resolver, node, &action_path),
    //        "expect:count" => self.execute_expect_count(action, value_resolver, node, &action_path),
    //        "join" => self.execute_join(action, value_resolver, node, &action_path),
    //        "join-with" => self.execute_join(action, value_resolver, node, &action_path),
    //        "error" => self.execute_error(action, value_resolver, node, &action_path),
    //        "header:parse" => self.execute_header_parse(action, value_resolver, node, &action_path),
    //        "for-each" => self.execute_for_each(value_resolver, node, &action_path),
    }
  }

  private fun executeMatch(
    action: String,
    matcher: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    return node
  }

  /** Push a result value onto the value stack */
  fun pushResult(value: NodeResult?) {
    valueStack.add(value)
  }

  /** Replace the top value of the stack with the new value */
  fun updateResult(value: NodeResult?) {
    val index = valueStack.lastIndex
    if (index >= 0) {
      valueStack[index] = value
    } else {
      valueStack.add(value)
    }
  }

  /** Return the value on the top if the stack */
  fun popResult(): NodeResult? {
    val index = valueStack.lastIndex
    return if (index >= 0) {
      valueStack.removeAt(index)
    } else {
      null
    }
  }

  /** Return the current stack value */
  fun stackValue() = valueStack.lastOrNull()

  fun resolveStackValue(path: DocPath): Result<NodeValue, String> {
    return when (val result = stackValue()) {
      null -> Result.Err("Can not resolve '$path', current value stack is either empty or contains an " +
        "empty value")

      is NodeResult.VALUE -> when (result.value) {
        is NodeValue.JSON -> {
          if (path.isRoot()) {
            Result.Ok(NodeValue.JSON(result.value.json))
          } else {
            val jsonPaths = JsonUtils.resolvePath(result.value.json, path)
            logger.trace { "resolved path $path -> $jsonPaths" }
            if (jsonPaths.isEmpty()) {
              Result.Ok(NodeValue.NULL)
            } else if (jsonPaths.size == 1) {
              when (val value = result.value.json.pointer(jsonPaths[0])) {
                JsonValue.Null -> Result.Ok(NodeValue.NULL)
                else -> Result.Ok(NodeValue.JSON(value))
              }
            } else {
              Result.Ok(NodeValue.JSON(JsonValue.Array(jsonPaths
                .map { result.value.json.pointer(it) }
                .toMutableList()
              )))
            }
          }
        }

        NodeValue.NULL -> Result.Err("Can not resolve '$path', current stack value does not contain a value" +
          " (is NULL)")

        // #[cfg(feature = "xml")]
//          NodeValue::XML(value) => {
//          if path.is_root() {
//            Ok(NodeValue::XML(value.clone()))
//          } else if let Some(element) = value.as_element() {
//            let xml_paths = pact_models::xml_utils::resolve_path(&element, path);
//            trace!("resolved path {} -> {:?}", path, xml_paths);
//            if xml_paths.is_empty() {
//              Ok(NodeValue::NULL)
//            } else if xml_paths.len() == 1 {
//              if let Some(value) = resolve_matching_node(&element, xml_paths[0].as_str()) {
//              Ok(NodeValue::XML(value.into()))
//            } else {
//              Ok(NodeValue::NULL)
//            }
//            } else {
//              let values = xml_paths.iter()
//                .map(|path| {
//                resolve_matching_node(&element, path.as_str())
//                .map(|node| NodeValue::XML(node.into()))
//                .unwrap_or_default()
//              })
//              .collect();
//              Ok(NodeValue::LIST(values))
//            }
//          } else {
//            todo!("Deal with other XML types: {}", value)
//          }

        else -> Result.Err("Can not resolve '$path', current stack value does not contain a value that is " +
          "resolvable (${result.value})")
      }

      else -> Result.Err("Can not resolve '$path', current stack value does not contain a value")
    }
  }
}
