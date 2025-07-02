package au.com.dius.pact.core.matchers.engine.interpreter

import au.com.dius.pact.core.matchers.engine.ExecutionPlanNode
import au.com.dius.pact.core.matchers.engine.NodeResult
import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.matchers.engine.PlanNodeType
import au.com.dius.pact.core.matchers.engine.orDefault
import au.com.dius.pact.core.matchers.engine.resolvers.ValueResolver
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.JsonUtils
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.handleWith
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import au.com.dius.pact.core.support.json.orNull
import au.com.dius.pact.core.support.isNotEmpty
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayInputStream

private val logger = KotlinLogging.logger {}

/**
 * Main interpreter for the matching plan AST
 */
@Suppress("LargeClass", "TooManyFunctions")
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
          is Result.Ok -> {
            logger.debug { "Resolved ${node.nodeType.path} -> ${result.value}" }
            node.copy(result = NodeResult.VALUE(result.value))
          }
          is Result.Err -> {
            logger.debug {
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

  @Suppress("CyclomaticComplexMethod")
  private fun executeAction(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    path: List<String>
  ): ExecutionPlanNode {
    logger.trace { "Executing action, action=$action" }

    val actionPath = path + action
    val result = if (action.startsWith("match:")) {
      when (val matcher = action.removePrefix("match:")) {
        "" -> node.copy(result = NodeResult.ERROR("'$action' is not a valid action"))
        else -> executeMatch(action, matcher, valueResolver, node, actionPath)
      }
    } else {
      when (action) {
        "upper-case" -> executeChangeCase(action, valueResolver, node, actionPath, true)
        "lower-case" -> executeChangeCase(action, valueResolver, node, actionPath, false)
        //        "to-string" => self.execute_to_string(action, valueResolver, node, actionPath),
        //        "length" => self.execute_length(action, valueResolver, node, actionPath),
        "expect:empty" -> executeExpectEmpty(action, valueResolver, node, actionPath)
        "convert:UTF8" -> executeConvertUtf8(action, valueResolver, node, actionPath)
        "if" -> executeIf(valueResolver, node, actionPath)
        //        "and" => self.execute_and(valueResolver, node, actionPath),
        //        "or" => self.execute_or(valueResolver, node, actionPath),
        "tee" -> executeTee(valueResolver, node, actionPath)
        //        "apply" => self.execute_apply(node),
        //        "push" => self.execute_push(node),
        //        "pop" => self.execute_pop(node),
        "json:parse" -> executeJsonParse(action, valueResolver, node, actionPath)
        //        #[cfg(feature = "xml")]
        //        "xml:parse" => self.execute_xml_parse(action, valueResolver, node, actionPath),
        //        #[cfg(feature = "xml")]
        //        "xml:tag-name" => self.execute_xml_tag_name(action, valueResolver, node, actionPath),
        //        #[cfg(feature = "xml")]
        //        "xml:value" => self.execute_xml_value(action, valueResolver, node, actionPath),
        //        #[cfg(feature = "xml")]
        //        "xml:attributes" => self.execute_xml_attributes(action, valueResolver, node, actionPath),
        "json:expect:empty" -> executeJsonExpectEmpty(action, valueResolver, node, actionPath)
        "json:match:length" -> executeJsonMatchLength(action, valueResolver, node, actionPath)
        "json:expect:entries" -> executeJsonExpectEntries(action, valueResolver, node, actionPath)
        "check:exists" -> executeCheckExists(action, valueResolver, node, actionPath)
        "expect:entries" -> executeCheckEntries(action, valueResolver, node, actionPath)
        "expect:only-entries" -> executeCheckEntries(action, valueResolver, node, actionPath)
        //        "expect:count" => self.execute_expect_count(action, valueResolver, node, actionPath),
        //        "join" => self.execute_join(action, valueResolver, node, actionPath),
        //        "join-with" => self.execute_join(action, valueResolver, node, actionPath),
        //        "error" => self.execute_error(action, valueResolver, node, actionPath),
        //        "header:parse" => self.execute_header_parse(action, valueResolver, node, actionPath),
        //        "for-each" => self.execute_for_each(valueResolver, node, actionPath),
        else -> node.copy(result = NodeResult.ERROR("'$action' is not a valid action"))
      }
    }

    logger.trace { "Executing $action -> ${result.result}" }
    return result
  }

  @Suppress("UnusedParameter")
  private fun executeChangeCase(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>,
    uppercase: Boolean
  ): ExecutionPlanNode {
    val (children, values) = when (val result = evaluateChildren(valueResolver, node, actionPath)) {
      is Result.Ok -> result.value
      is Result.Err -> return result.error
    }

    val results = values.map { value ->
      when (value) {
        is NodeValue.JSON -> when (val json = value.json) {
          is JsonValue.StringValue -> NodeValue.STRING(
            if (uppercase) json.value.toString().uppercase()
            else json.value.toString().lowercase())
          else -> NodeValue.STRING(value.json.serialise())
        }
        is NodeValue.SLIST -> NodeValue.SLIST(value.items.map {
          if (uppercase) it.uppercase() else it.lowercase()
        })
        is NodeValue.STRING -> NodeValue.STRING(
          if (uppercase) value.string.uppercase() else value.string.lowercase())
        else -> value
      }
    }

    val result = if (results.size == 1) {
      results[0]
    } else {
      NodeValue.LIST(results)
    }
    return node.copy(result = NodeResult.VALUE(result), children = children.toMutableList())
  }

  @Suppress("ReturnCount")
  private fun executeMatch(
    action: String,
    matcher: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    when (val result = validateArgs(3, 1, node, action, valueResolver, actionPath)) {
      is Result.Ok -> {
        val (args, optional) = result.value
        val firstNode = args[0]
        val secondNode = args[1]
        val thirdNode = args[2]

        val expectedValue = when(val v = firstNode.value().orDefault().valueOrError()) {
          is Result.Ok -> v.value
          is Result.Err -> return node.copy(result = NodeResult.ERROR(v.error),
            children = (listOf(firstNode, secondNode, thirdNode) + optional).toMutableList())
        }
        val actualValue = when(val v = secondNode.value().orDefault().valueOrError()) {
          is Result.Ok -> v.value
          is Result.Err -> return node.copy(result = NodeResult.ERROR(v.error),
            children = (listOf(firstNode, secondNode, thirdNode) + optional).toMutableList())
        }
        val matcherParams = when(val v = thirdNode.value().orDefault().valueOrError()) {
          is Result.Ok -> v.value.asJson().orNull()
          is Result.Err -> return node.copy(result = NodeResult.ERROR(v.error),
            children = (listOf(firstNode, secondNode, thirdNode) + optional).toMutableList())
        }

        when (val result = handleWith<MatchingRule> { MatchingRule.create(matcher, matcherParams) }) {
          is Result.Ok -> {
            val matchResult = NodeValue.doMatch(expectedValue, actualValue, result.value, false, actionPath)
            if (matchResult == null) {
              return node.copy(result = NodeResult.VALUE(NodeValue.BOOL(true)),
                children = (listOf(firstNode, secondNode, thirdNode) + optional).toMutableList())
            } else {
              val errorNode = optional.firstOrNull()
              if (errorNode != null) {
                pushResult(NodeResult.ERROR(matchResult))

                val errorNodeResult = walkTree(actionPath, errorNode, valueResolver)
                val message = errorNodeResult.value()?.asString()
                return if (message.isNotEmpty()) {
                  node.copy(result = NodeResult.ERROR(message!!),
                    children = (listOf(firstNode, secondNode, thirdNode) + optional).toMutableList())
                } else {
                  // There was an error generating the optional message, so just return the original error
                  node.copy(result = NodeResult.ERROR(matchResult),
                    children = (listOf(firstNode, secondNode, thirdNode) + optional).toMutableList())
                }
              } else {
                return node.copy(result = NodeResult.ERROR(matchResult),
                  children = (listOf(firstNode, secondNode, thirdNode) + optional).toMutableList())
              }
            }
          }
          is Result.Err -> return node.copy(result = NodeResult.ERROR(result.error.message!!))
        }
      }
      is Result.Err -> return node.copy(result = NodeResult.ERROR(result.error))
    }
  }

  @Suppress("LongMethod", "CyclomaticComplexMethod", "NestedBlockDepth")
  private fun executeExpectEmpty(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    return when (val result = validateArgs(1, 1, node, action, valueResolver, actionPath)) {
      is Result.Ok -> {
        val (args, optional) = result.value

        val firstArg = args[0].value().orDefault()
        if (firstArg is NodeResult.ERROR) {
          node.copy(result = NodeResult.ERROR(firstArg.message),
            children = (args + optional).toMutableList())
        } else {
          val argValue = firstArg.asValue()
          val result = if (argValue != null) {
            when (argValue) {
              is NodeValue.BARRAY -> {
                if (argValue.bytes.isEmpty()) {
                  Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
                } else {
                  Result.Err("Expected byte array (${argValue.bytes.size} bytes) to be empty")
                }
              }
              is NodeValue.BOOL -> Result.Ok(NodeResult.VALUE(NodeValue.BOOL(argValue.bool)))
              is NodeValue.ENTRY -> Result.Ok(NodeResult.VALUE(NodeValue.BOOL(false)))
              is NodeValue.JSON -> {
                when (argValue.json) {
                  is JsonValue.Array -> {
                    if (argValue.json.values.isEmpty()) {
                      Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
                    } else {
                      Result.Err("Expected JSON Array ${argValue.json.values} to be empty")
                    }
                  }
                  JsonValue.Null -> Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
                  is JsonValue.Object -> {
                    if (argValue.json.entries.isEmpty()) {
                      Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
                    } else {
                      Result.Err("Expected JSON Object ${argValue.json.entries} to be empty")
                    }
                  }
                  is JsonValue.StringValue -> {
                    if (argValue.json.value.chars.isEmpty()) {
                      Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
                    } else {
                      Result.Err("Expected JSON String ${argValue.json.value} to be empty")
                    }
                  }
                  else -> Result.Err("Expected json (${argValue.json.serialise()}) to be empty")
                }
              }
              is NodeValue.LIST -> {
                if (argValue.items.isEmpty()) {
                  Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
                } else {
                  Result.Err("Expected ${argValue.items} to be empty")
                }
              }
              is NodeValue.MMAP -> {
                if (argValue.entries.isEmpty()) {
                  Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
                } else {
                  Result.Err("Expected ${argValue.entries} to be empty")
                }
              }
              is NodeValue.NAMESPACED -> TODO("Not Implemented: Need a way to resolve NodeValue::NAMESPACED")
              NodeValue.NULL -> Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
              is NodeValue.SLIST -> {
                if (argValue.items.isEmpty()) {
                  Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
                } else {
                  Result.Err("Expected ${argValue.items} to be empty")
                }
              }
              is NodeValue.STRING -> {
                if (argValue.string.isEmpty()) {
                  Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
                } else {
                  Result.Err("Expected '${argValue.string}' to be empty")
                }
              }
              is NodeValue.UINT -> {
                if (argValue.uint.toInt() == 0) {
                  Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
                } else {
                  Result.Err("Expected ${argValue.uint} to be empty")
                }
              }
              //              #[cfg(feature = "xml")]
              //              NodeValue::XML(xml) => match xml {
              //                XmlValue::Element(element) => if element.child_elements().next().is_none() {
              //                  Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
              //                } else {
              //                  Err(anyhow!("Expected {} to be empty", element))
              //                }
              //                XmlValue::Text(text) => if text.is_empty() {
              //                  Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
              //                } else {
              //                  Err(anyhow!("Expected {:?} to be empty", value))
              //                }
              //                XmlValue::Attribute(name, value) => if value.is_empty() {
              //                  Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
              //                } else {
              //                  Err(anyhow!("Expected {}={} to be empty", name, value))
              //                }
              //              }
            }
          } else {
            Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
          }

          when (result) {
            is Result.Ok -> node.copy(result = result.value, children = (args + optional).toMutableList())
            is Result.Err -> {
              logger.debug { "expect:empty failed with an error: ${result.error}" }
              val errorNode = optional.firstOrNull()
              if (errorNode != null) {
                val errorNodeResult = walkTree(actionPath, errorNode, valueResolver)
                val message = errorNodeResult.value()?.asString()
                return if (message.isNotEmpty()) {
                  node.copy(result = NodeResult.ERROR(message!!),
                    children = (args + optional).toMutableList())
                } else {
                  // There was an error generating the optional message, so just return the original error
                  node.copy(result = NodeResult.ERROR(result.error), children = (args + optional).toMutableList())
                }
              } else {
                node.copy(result = NodeResult.ERROR(result.error), children = (args + optional).toMutableList())
              }
            }
          }
        }
      }
      is Result.Err -> node.copy(result = NodeResult.ERROR(result.error))
    }
  }

  private fun executeIf(valueResolver: ValueResolver, node: ExecutionPlanNode, actionPath: List<String>): ExecutionPlanNode {
    val firstNode = node.children.firstOrNull()
    return if (firstNode != null) {
      val result = walkTree(actionPath, firstNode, valueResolver)
      when (result.result) {
        is NodeResult.ERROR -> node.copy(result = result.result,
          children = (listOf(result) + node.children.drop(1)).toMutableList())
        else -> {
          val nodeResult = result.value().orDefault()
          val children = mutableListOf(result)
          if (!nodeResult.isTruthy()) {
            val thirdNode = node.children.getOrNull(2)
            if (thirdNode != null) {
              val elseResult = walkTree(actionPath, thirdNode, valueResolver)
              children.add(node.children[1])
              children.add(elseResult)
              node.copy(result = elseResult.result, children = children)
            } else {
              node.copy(result = NodeResult.VALUE(NodeValue.BOOL(false)),
                children = (listOf(result) + node.children.drop(1)).toMutableList())
            }
          } else {
            val secondNode = node.children.getOrNull(1)
            if (secondNode != null) {
              val ifResult = walkTree(actionPath, secondNode, valueResolver)
              children.add(ifResult)
              children.addAll(node.children.drop(2))
              node.copy(result = ifResult.result.orDefault().truthy(), children = children)
            } else {
              node.copy(result = result.result,
                children = (listOf(result) + node.children.drop(1)).toMutableList())
            }
          }
        }
      }
    } else {
      node.copy(result = NodeResult.ERROR("'if' action requires at least one argument"))
    }
  }

  private fun executeConvertUtf8(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    return when (val resultNode = validateOneArg(node, action, valueResolver, actionPath)) {
      is Result.Ok -> {
        val argValue = resultNode.value.value().orDefault().asValue()
        val result = if (argValue != null) {
          when (argValue) {
            is NodeValue.BARRAY -> Result.Ok(NodeResult.VALUE(NodeValue.STRING(argValue.bytes.decodeToString())))
            NodeValue.NULL -> Result.Ok(NodeResult.VALUE(NodeValue.STRING("")))
            is NodeValue.STRING -> Result.Ok(NodeResult.VALUE(NodeValue.STRING(argValue.string)))
            else -> Result.Err("convert:UTF8 can not be used with ${argValue.valueType()}")
          }
        } else {
          Result.Ok(NodeResult.VALUE(NodeValue.STRING("")))
        }
        when (result) {
          is Result.Ok -> node.copy(result = result.value, children = mutableListOf(resultNode.value))
          is Result.Err -> node.copy(result = NodeResult.ERROR(result.error))
        }
      }
      is Result.Err -> node.copy(result = NodeResult.ERROR(resultNode.error))
    }
  }

  private fun executeTee(
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    val firstChild = node.children.firstOrNull()
    return if (firstChild != null) {
      val firstResult = walkTree(actionPath, firstChild, valueResolver)
      when (firstResult.result) {
        is NodeResult.ERROR -> node.copy(result = firstResult.result,
          children = (listOf(firstResult) + node.children.drop(1)).toMutableList())
        else -> {
          pushResult(firstResult.result)
          var result: NodeResult = NodeResult.OK
          val childResults = mutableListOf(firstResult)
          for (child in node.children.drop(1)) {
            val childResult = walkTree(actionPath, child, valueResolver)
            result = result.and(childResult.result)
            childResults.add(childResult)
          }

          popResult()
          node.copy(result = result.truthy(), children = childResults)
        }
      }
    } else {
      node.copy(result = NodeResult.OK)
    }
  }

  private fun executeJsonParse(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    return when (val resultNode = validateOneArg(node, action, valueResolver, actionPath)) {
      is Result.Ok -> {
        val argValue = resultNode.value.value().orDefault().asValue()
        val result = if (argValue != null) {
          when (argValue) {
            is NodeValue.BARRAY -> {
              val jsonResult = handleWith<JsonValue> {
                JsonParser.parseStream(ByteArrayInputStream(argValue.bytes))
              }
              when (jsonResult) {
                is Result.Ok -> Result.Ok(NodeResult.VALUE(NodeValue.JSON(jsonResult.value)))
                is Result.Err -> Result.Err("json parse error: ${jsonResult.error.message}")
              }
            }
            NodeValue.NULL -> Result.Ok(NodeResult.VALUE(NodeValue.NULL))
            is NodeValue.STRING -> {
              val jsonResult = handleWith<JsonValue> {
                JsonParser.parseString(argValue.string)
              }
              when (jsonResult) {
                is Result.Ok -> Result.Ok(NodeResult.VALUE(NodeValue.JSON(jsonResult.value)))
                is Result.Err -> Result.Err("json parse error: ${jsonResult.error.message}")
              }
            }
            else -> Result.Err("json:parse can not be used with ${argValue.valueType()}")
          }
        } else {
          Result.Ok(NodeResult.VALUE(NodeValue.NULL))
        }
        when (result) {
          is Result.Ok -> node.copy(result = result.value, children = mutableListOf(resultNode.value))
          is Result.Err -> node.copy(result = NodeResult.ERROR(result.error),
            children = mutableListOf(resultNode.value))
        }
      }
      is Result.Err -> node.copy(result = NodeResult.ERROR(resultNode.error))
    }
  }

  @Suppress("CyclomaticComplexMethod")
  private fun executeJsonExpectEmpty(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    return when (val result = validateTwoArgs(node, action, valueResolver, actionPath)) {
      is Result.Ok -> {
        val (first, second) = result.value

        val result1 = first.value().orDefault()
        val expectedJsonType = result1.asString()
        if (expectedJsonType == null) {
          return node.copy(result = NodeResult.ERROR("'${result1}' is not a valid JSON type"),
            children = mutableListOf(first, second))
        }

        val result2 = second.value().orDefault()
        val value = result2.asValue()
        if (value == null) {
          return node.copy(result = NodeResult.ERROR("Was expecting a JSON value, but got '${result2}'"),
            children = mutableListOf(first, second))
        }
        val jsonValue = value.asJson()
        if (jsonValue == null) {
          return node.copy(result = NodeResult.ERROR("Was expecting a JSON value, but got '${value}'"),
            children = mutableListOf(first, second))
        }

        val checkResult = jsonCheckType(expectedJsonType, jsonValue)
        if (checkResult != null) {
          return node.copy(result = NodeResult.ERROR(checkResult),
            children = mutableListOf(first, second))
        }

        val error = when (jsonValue) {
          is JsonValue.Array -> if (jsonValue.values.isEmpty()) null
            else "Expected JSON Array (${jsonValue}) to be empty"
          JsonValue.Null -> null
          is JsonValue.Object -> if (jsonValue.entries.isEmpty()) null
            else "Expected JSON Object (${jsonValue}) to be empty"
          is JsonValue.StringValue -> if (jsonValue.value.chars.isEmpty()) null
            else "Expected JSON String (${jsonValue}) to be empty"
          else -> "Expected json (${jsonValue}) to be empty"
        }
        if (error == null) {
          node.copy(result = NodeResult.VALUE(NodeValue.BOOL(true)),
            children = mutableListOf(first, second))
        } else {
          node.copy(result = NodeResult.ERROR(error),
            children = mutableListOf(first, second))
        }
      }
      is Result.Err -> node.copy(result = NodeResult.ERROR(result.error))
    }
  }

  private fun executeJsonMatchLength(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    return when (val result = validateThreeArgs(node, action, valueResolver, actionPath)) {
      is Result.Ok -> {
        val (first, second, third) = result.value

        val result1 = first.value().orDefault()
        val expectedJsonType = result1.asString()
        if (expectedJsonType == null) {
          return node.copy(result = NodeResult.ERROR("'${result1}' is not a valid JSON type"),
            children = mutableListOf(first, second, third))
        }

        val result2 = second.value().orDefault()
        val expectedLength = result2.asNumber()
        if (expectedLength == null) {
          return node.copy(result = NodeResult.ERROR("'${result1}' is not a valid number"),
            children = mutableListOf(first, second, third))
        }

        val result3 = third.value().orDefault()
        val value = result3.asValue()
        if (value == null) {
          return node.copy(result = NodeResult.ERROR("Was expecting a JSON value, but got '${result3}'"),
            children = mutableListOf(first, second, third))
        }
        val jsonValue = value.asJson()
        if (jsonValue == null) {
          return node.copy(result = NodeResult.ERROR("Was expecting a JSON value, but got '${value}'"),
            children = mutableListOf(first, second, third))
        }

        val checkResult = jsonCheckType(expectedJsonType, jsonValue)
        if (checkResult != null) {
          return node.copy(result = NodeResult.ERROR(checkResult),
            children = mutableListOf(first, second, third))
        }

        val checkLengthResult = jsonCheckLength(expectedLength.toInt(), jsonValue)
        if (checkLengthResult != null) {
          node.copy(result = NodeResult.ERROR(checkLengthResult),
            children = mutableListOf(first, second, third))
        } else {
          node.copy(result = NodeResult.VALUE(NodeValue.BOOL(true)),
            children = mutableListOf(first, second, third))
        }
      }

      is Result.Err -> node.copy(result = NodeResult.ERROR(result.error))
    }
  }

  private fun executeJsonExpectEntries(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    return when (val result = validateThreeArgs(node, action, valueResolver, actionPath)) {
      is Result.Ok -> {
        val (first, second, third) = result.value

        val result1 = first.value().orDefault()
        val expectedJsonType = result1.asString()
        if (expectedJsonType == null) {
          return node.copy(result = NodeResult.ERROR("'${result1}' is not a valid JSON type"),
            children = mutableListOf(first, second, third))
        }

        val result2 = second.value().orDefault()
        val keys = result2.asSList()
        if (keys == null) {
          return node.copy(result = NodeResult.ERROR("'${result1}' is not a list of strings"),
            children = mutableListOf(first, second, third))
        }
        val expectedKeys = keys.toSet()

        val result3 = third.value().orDefault()
        val value = result3.asValue()
        if (value == null) {
          return node.copy(result = NodeResult.ERROR("Was expecting a JSON value, but got '${result3}'"),
            children = mutableListOf(first, second, third))
        }
        val jsonValue = value.asJson()
        if (jsonValue == null) {
          return node.copy(result = NodeResult.ERROR("Was expecting a JSON value, but got '${value}'"),
            children = mutableListOf(first, second, third))
        }

        val checkResult = jsonCheckType(expectedJsonType, jsonValue)
        if (checkResult != null) {
          return node.copy(result = NodeResult.ERROR(checkResult),
            children = mutableListOf(first, second, third))
        }

        when (val obj = jsonValue.asObject()) {
          is JsonValue.Object -> {
            val actualKeys = obj.entries.keys
            val diff = expectedKeys - actualKeys
            if (diff.isEmpty()) {
              node.copy(result = NodeResult.VALUE(NodeValue.BOOL(true)),
                children = mutableListOf(first, second, third))
            } else {
              node.copy(result = NodeResult.ERROR(
                "The following expected entries were missing from the actual object: ${diff.joinToString(", ")}"),
                children = mutableListOf(first, second, third))
            }
          }
          else -> {
            node.copy(result = NodeResult.ERROR("Was expecting a JSON Object, but got ${jsonValue}"),
              children = mutableListOf(first, second, third))
          }
        }
      }

      is Result.Err -> node.copy(result = NodeResult.ERROR(result.error))
    }
  }

  private fun executeCheckExists(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    return when (val resultNode = validateOneArg(node, action, valueResolver, actionPath)) {
      is Result.Ok -> {
        val argValue = resultNode.value.value().orDefault().asValue()
        val result = if (argValue != null) {
          when (argValue) {
            NodeValue.NULL -> NodeResult.VALUE(NodeValue.BOOL(false))
            else -> NodeResult.VALUE(NodeValue.BOOL(true))
          }
        } else {
          NodeResult.VALUE(NodeValue.BOOL(false))
        }
        node.copy(result = result, children = mutableListOf(resultNode.value))
      }
      is Result.Err -> node.copy(result = NodeResult.ERROR(resultNode.error))
    }
  }

  @Suppress("LongMethod", "CyclomaticComplexMethod")
  private fun executeCheckEntries(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    return when (val result = validateArgs(2, 1, node, action, valueResolver, actionPath)) {
      is Result.Ok -> {
        val expectedKeys = result.value.first[0].value()
          .orDefault()
          .asValue()
          .orDefault()
          .asSList()
          ?.toSet()
          ?: emptySet()

        val second = result.value.first[1].value()
          .orDefault()
          .asValue()
          .orDefault()

        val actionResult = when (second) {
          is NodeValue.JSON -> {
            when (second.json) {
              is JsonValue.Array -> checkDiff(action, expectedKeys, second.json.values.
                map { it.toString() }.toSet())
              is JsonValue.Object -> checkDiff(action, expectedKeys, second.json.entries.keys)
              else -> "'$action' can't be used with a $second node" to null
            }
          }
          is NodeValue.MMAP -> checkDiff(action, expectedKeys, second.entries.keys)
          is NodeValue.SLIST -> checkDiff(action, expectedKeys, second.items.toSet())
          is NodeValue.STRING -> checkDiff(action, expectedKeys, setOf(second.string))
          //          #[cfg(feature = "xml")]
          //          NodeValue::XML(xml) => match xml {
          //            XmlValue::Element(element) => {
          //              let actual_keys = element.child_elements()
          //                .map(|child| child.name())
          //                .collect::<HashSet<_>>();
          //              Self::check_diff(action, &expected_keys, &actual_keys)
          //            }
          //            _ => Err((format!("'{}' can't be used with a {:?} node", action, second), None))
          //          }
          else -> "'$action' can't be used with a $second node" to null
        }

        if (actionResult == null) {
          node.copy(result = NodeResult.OK, children = (result.value.first + result.value.second).toMutableList())
        } else {
          logger.debug { "expect:empty failed with an error: ${actionResult.first}" }
          if (result.value.second.isNotEmpty()) {
            if (actionResult.second != null) {
              pushResult(NodeResult.VALUE(NodeValue.SLIST(actionResult.second!!.toList())))
              val errorNodeResult = walkTree(actionPath, result.value.second[0], valueResolver)
              when (val messageValue = errorNodeResult.value().orDefault()) {
                is NodeResult.VALUE -> {
                  val message = messageValue.orDefault().asString() ?: ""
                  node.copy(result = NodeResult.ERROR(message),
                    children = (result.value.first + result.value.second).toMutableList())
                }
                else -> {
                  // There was an error generating the optional message, so just return the
                  // original error
                  popResult()
                  node.copy(result = NodeResult.ERROR(actionResult.first),
                    children = (result.value.first + result.value.second).toMutableList())
                }
              }
            } else {
              node.copy(result = NodeResult.ERROR(actionResult.first),
                children = (result.value.first + result.value.second).toMutableList())
            }
          } else {
            node.copy(result = NodeResult.ERROR(actionResult.first),
              children = (result.value.first + result.value.second).toMutableList())
          }
        }
      }
      is Result.Err -> node.copy(result = NodeResult.ERROR(result.error))
    }
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

  private fun evaluateChildren(
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    path: List<String>
  ): Result<Pair<List<ExecutionPlanNode>, List<NodeValue>>, ExecutionPlanNode> {
    val children = mutableListOf<ExecutionPlanNode>()
    val values = mutableListOf<NodeValue>()
    val loopItems = ArrayDeque(node.children)

    while (loopItems.isNotEmpty()) {
      val child = loopItems.removeFirst()

      var nodeResult = child.value()
      if (nodeResult == null) {
        val result = walkTree(path, child, valueResolver)
        when (result.result) {
          is NodeResult.ERROR -> {
            children.add(result)
            children.addAll(loopItems)
            return Result.Err(node.copy(result = result.result, children = children))
          }
          else -> {
            if (result.isSplat()) {
              children.add(result)
              for (splatChild in result.children.reversed()) {
                loopItems.addFirst(splatChild)
              }
              nodeResult = NodeResult.OK
            } else {
              children.add(result)
              nodeResult = result.result ?: NodeResult.OK
            }
          }
        }
      }

      if (nodeResult is NodeResult.VALUE) {
        values.add(nodeResult.value)
      }
    }

    return Result.Ok(children to values)
  }

  @Suppress("LongParameterList")
  private fun validateArgs(
    required: Int,
    optional: Int,
    node: ExecutionPlanNode,
    action: String,
    valueResolver: ValueResolver,
    path: List<String>
  ): Result<Pair<List<ExecutionPlanNode>, List<ExecutionPlanNode>>, String> {
    return if (node.children.size < required) {
      Result.Err("$action requires $required arguments, got ${node.children.size}")
    } else if (node.children.size > required + optional) {
      Result.Err("$action supports at most ${required + optional} arguments, got ${node.children.size}")
    } else {
      val requiredArgs = mutableListOf<ExecutionPlanNode>()
      for (child in node.children.take(required)) {
        val value = walkTree(path, child, valueResolver)
        requiredArgs.add(value)
      }
      Result.Ok(requiredArgs to node.children.drop(required))
    }
  }

  private fun validateOneArg(
    node: ExecutionPlanNode,
    action: String,
    valueResolver: ValueResolver,
    path: List<String>
  ): Result<ExecutionPlanNode, String> {
    return if (node.children.size > 1) {
      Result.Err("$action takes only one argument, got ${node.children.size}")
    } else if (node.children.isEmpty()) {
      Result.Err("$action requires one argument, got none")
    } else {
      Result.Ok(walkTree(path, node.children[0], valueResolver))
    }
  }

  private fun validateTwoArgs(
    node: ExecutionPlanNode,
    action: String,
    valueResolver: ValueResolver,
    path: List<String>
  ): Result<Pair<ExecutionPlanNode, ExecutionPlanNode>, String> {
    return if (node.children.size == 2) {
      val first = walkTree(path, node.children[0], valueResolver)
      val second = walkTree(path, node.children[1], valueResolver)
      Result.Ok(first to second)
    } else {
      Result.Err("$action requires two arguments, got ${node.children.size}")
    }
  }

  private fun validateThreeArgs(
    node: ExecutionPlanNode,
    action: String,
    valueResolver: ValueResolver,
    path: List<String>
  ): Result<Triple<ExecutionPlanNode, ExecutionPlanNode, ExecutionPlanNode>, String> {
    return if (node.children.size == 3) {
      val first = walkTree(path, node.children[0], valueResolver)
      val second = walkTree(path, node.children[1], valueResolver)
      val third = walkTree(path, node.children[2], valueResolver)
      Result.Ok(Triple(first, second, third))
    } else {
      Result.Err("$action requires three arguments, got ${node.children.size}")
    }
  }

  private fun jsonCheckType(expectedType: String, jsonValue: JsonValue): String? {
    return when (expectedType) {
      "NULL" -> if (jsonValue.isNull) null
        else "Was expecting a JSON NULL but got a ${jsonValue.type()}"
      "BOOL" -> if (jsonValue.isBoolean) null
        else "Was expecting a JSON Boolean but got a ${jsonValue.type()}"
      "NUMBER" -> if (jsonValue.isNumber) null
        else "Was expecting a JSON Number but got a ${jsonValue.type()}"
      "STRING" -> if (jsonValue.isString) null
        else "Was expecting a JSON String but got a ${jsonValue.type()}"
      "ARRAY" -> if (jsonValue.isArray) null
        else "Was expecting a JSON Array but got a ${jsonValue.type()}"
      "OBJECT" -> if (jsonValue.isObject) null
        else "Was expecting a JSON Object but got a ${jsonValue.type()}"
      else -> "'${expectedType}' is not a valid JSON type"
    }
  }

  private fun jsonCheckLength(length: Int, json: JsonValue): String? {
    return when (json) {
      is JsonValue.Array -> if (json.values.size == length) null
        else "Was expecting a length of ${length}, but actual length is ${json.values.size}"
      is JsonValue.Object -> if (json.entries.size == length) null
        else "Was expecting a length of ${length}, but actual length is ${json.entries.size}"
      else -> null
    }
  }

  private fun checkDiff(
    action: String,
    expectedKeys: Set<String>,
    actualKeys: Set<String>
  ): Pair<String, Set<String>?>? {
    return when (action) {
      "expect:entries" -> {
        val diff = expectedKeys - actualKeys
        if (diff.isEmpty()) {
          null
        } else {
          val keys = NodeValue.SLIST(diff.toList())
          "The following expected entries were missing: $keys" to diff
        }
      }
      "expect:only-entries" -> {
        val diff = actualKeys - expectedKeys
        if (diff.isEmpty()) {
          null
        } else {
          val keys = NodeValue.SLIST(diff.toList())
          "The following unexpected entries were received: $keys" to diff
        }
      }
      else -> "'$action' is not a valid action" to null
    }
  }
}

