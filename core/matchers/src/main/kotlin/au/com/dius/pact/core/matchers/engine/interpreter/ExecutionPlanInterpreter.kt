package au.com.dius.pact.core.matchers.engine.interpreter

import au.com.dius.pact.core.matchers.engine.ExecutionPlanNode
import au.com.dius.pact.core.matchers.engine.NodeResult
import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.NodeValue.Companion.escape
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.matchers.engine.PlanNodeType
import au.com.dius.pact.core.matchers.engine.XmlValue
import au.com.dius.pact.core.matchers.engine.orDefault
import au.com.dius.pact.core.matchers.engine.resolvers.ValueResolver
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.HeaderParser.headerValueToMap
import au.com.dius.pact.core.model.JsonUtils
import au.com.dius.pact.core.model.PathToken
import au.com.dius.pact.core.model.XmlUtils.attributes
import au.com.dius.pact.core.model.XmlUtils.childElements
import au.com.dius.pact.core.model.XmlUtils.groupChildren
import au.com.dius.pact.core.model.XmlUtils.parse
import au.com.dius.pact.core.model.XmlUtils.renderXml
import au.com.dius.pact.core.model.XmlUtils.resolveMatchingNode
import au.com.dius.pact.core.model.XmlUtils.resolvePath
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.handleWith
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import au.com.dius.pact.core.support.json.orNull
import au.com.dius.pact.core.support.isNotEmpty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream

private val logger = KotlinLogging.logger {}

/**
 * Main interpreter for the matching plan AST
 */
@Suppress("LargeClass", "TooManyFunctions", "ReturnCount", "UnusedParameter")
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
              "xml" -> {
                when (val xmlResult = handleWith<Node> { parse(v.value) }) {
                  is Result.Ok -> {
                    when (val xml = XmlValue.fromNode(xmlResult.value)) {
                      is Result.Ok -> NodeResult.VALUE(NodeValue.XML(xml.value))
                      is Result.Err -> NodeResult.ERROR(xml.error)
                    }
                  }
                  is Result.Err -> NodeResult.ERROR(xmlResult.error.message!!)
                }
              }
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
        "to-string" -> executeToString(action, valueResolver, node, actionPath)
        "length" -> executeLength(action, valueResolver, node, actionPath)
        "expect:empty" -> executeExpectEmpty(action, valueResolver, node, actionPath)
        "convert:UTF8" -> executeConvertUtf8(action, valueResolver, node, actionPath)
        "if" -> executeIf(valueResolver, node, actionPath)
        "and" -> executeAnd(valueResolver, node, actionPath)
        "or" -> executeOr(valueResolver, node, actionPath)
        "tee" -> executeTee(valueResolver, node, actionPath)
        "apply" -> executeApply(node)
        //        "push" => self.execute_push(node),
        //        "pop" => self.execute_pop(node),
        "json:parse" -> executeJsonParse(action, valueResolver, node, actionPath)
        "xml:parse" -> executeXmlParse(action, valueResolver, node, actionPath)
        //        #[cfg(feature = "xml")]
        //        "xml:tag-name" => self.execute_xml_tag_name(action, valueResolver, node, actionPath),
        "xml:value" -> executeXmlValue(action, valueResolver, node, actionPath)
        "xml:attributes" -> executeXmlAttributes(action, valueResolver, node, actionPath)
        "json:expect:empty" -> executeJsonExpectEmpty(action, valueResolver, node, actionPath)
        "json:match:length" -> executeJsonMatchLength(action, valueResolver, node, actionPath)
        "json:expect:entries" -> executeJsonExpectEntries(action, valueResolver, node, actionPath)
        "check:exists" -> executeCheckExists(action, valueResolver, node, actionPath)
        "expect:entries" -> executeCheckEntries(action, valueResolver, node, actionPath)
        "expect:only-entries" -> executeCheckEntries(action, valueResolver, node, actionPath)
        "expect:count" -> executeExpectCount(action, valueResolver, node, actionPath)
        "join" -> executeJoin(action, valueResolver, node, actionPath)
        "join-with" -> executeJoin(action, valueResolver, node, actionPath)
        "error" -> executeError(action, valueResolver, node, actionPath)
        "header:parse" -> executeHeaderParse(action, valueResolver, node, actionPath)
        "for-each" -> executeForEach(valueResolver, node, actionPath)
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

    val results = values.map { v ->
      when (val value = v.asValue().orDefault()) {
        is NodeValue.JSON -> when (val json = value.json) {
          is JsonValue.StringValue -> NodeValue.STRING(
            if (uppercase) json.toString().uppercase()
            else json.toString().lowercase())
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
            val matchResult = NodeValue.doMatch(expectedValue, actualValue, result.value, false, actionPath, context)
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
              is NodeValue.XML -> when (val xml = argValue.xml) {
                is XmlValue.Attribute -> if (xml.value.isEmpty()) {
                  Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
                } else {
                  Result.Err("Expected ${xml.name}=${xml.value} to be empty")
                }
                is XmlValue.Element -> if (childElements(xml.element).isEmpty()) {
                  Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
                } else {
                  Result.Err("Expected ${xml.element} to be empty")
                }
                is XmlValue.Text -> if (xml.text.isEmpty()) {
                  Result.Ok(NodeResult.VALUE(NodeValue.BOOL(true)))
                } else {
                  Result.Err("Expected '${xml.text}' to be empty")
                }
              }
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
                    children = (args + errorNodeResult).toMutableList())
                } else {
                  // There was an error generating the optional message, so just return the original error
                  node.copy(result = NodeResult.ERROR(result.error),
                    children = (args + errorNodeResult).toMutableList())
                }
              } else {
                node.copy(result = NodeResult.ERROR(result.error),
                  children = (args + optional).toMutableList())
              }
            }
          }
        }
      }
      is Result.Err -> node.copy(result = NodeResult.ERROR(result.error))
    }
  }

  private fun executeIf(
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
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
          is NodeValue.XML -> {
            when (val xml = second.xml) {
              is XmlValue.Element -> {
                val actualKeys = groupChildren(xml.element).keys
                checkDiff(action, expectedKeys, actualKeys)
              }
              else -> "'$action' can't be used with a $second node" to null
            }
          }
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
              popResult()
              when (val messageValue = errorNodeResult.value().orDefault()) {
                is NodeResult.VALUE -> {
                  val message = messageValue.orDefault().asString() ?: ""
                  node.copy(result = NodeResult.ERROR(message),
                    children = (result.value.first + errorNodeResult).toMutableList())
                }
                else -> {
                  // There was an error generating the optional message, so just return the
                  // original error
                  node.copy(result = NodeResult.ERROR(actionResult.first),
                    children = (result.value.first + errorNodeResult).toMutableList())
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

  private fun executeJoin(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    val (children, values) = when (val result = evaluateChildren(valueResolver, node, actionPath)) {
      is Result.Ok -> {
        result.value.first to result.value.second.flatMap { value ->
          when (val v = value.asValue().orDefault()) {
            is NodeValue.BARRAY -> listOf(v.strForm())
            is NodeValue.BOOL -> listOf(v.bool.toString())
            is NodeValue.JSON -> listOf(v.json.toString())
            is NodeValue.MMAP -> listOf(v.strForm())
            is NodeValue.NAMESPACED -> listOf(v.strForm())
            is NodeValue.SLIST -> v.items
            is NodeValue.STRING -> listOf(v.string)
            is NodeValue.UINT -> listOf(v.uint.toString())
            else -> emptyList()
          }
        }
      }
      is Result.Err -> return result.error
    }

    val result = if (action == "join-with" && values.isNotEmpty()) {
      val first = values[0]
      values.drop(1).joinToString(first)
    } else {
      values.joinToString("")
    }

    return node.copy(result = NodeResult.VALUE(NodeValue.STRING(result)),
      children = children.toMutableList())
  }

  private fun executeApply(node: ExecutionPlanNode): ExecutionPlanNode {
    return if (valueStack.isNotEmpty()) {
      node.copy(result = valueStack.last())
    } else {
      node.copy(result = NodeResult.ERROR("No value to apply (stack is empty)"))
    }
  }

  private fun executeHeaderParse(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    return when (val resultNode = validateOneArg(node, action, valueResolver, actionPath)) {
      is Result.Ok -> {
        val argValue = resultNode.value.value().orDefault().asString() ?: ""
        val (headerValue, headerParams) = headerValueToMap(argValue)
        val result = NodeResult.VALUE(NodeValue.JSON(JsonValue.Object(mutableMapOf(
          "value" to JsonValue.StringValue(headerValue),
          "parameters" to Json.toJson(headerParams)
        ))))
        node.copy(result = result, children = mutableListOf(resultNode.value))
      }
      is Result.Err -> node.copy(result = NodeResult.ERROR(resultNode.error))
    }
  }

  @Suppress("CyclomaticComplexMethod")
  private fun executeToString(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    val (children, values) = when (val result = evaluateChildren(valueResolver, node, actionPath)) {
      is Result.Ok -> {
        result.value.first to result.value.second.map { v ->
          when (val value = v.asValue().orDefault()) {
            is NodeValue.JSON -> when (val json = value.json) {
              is JsonValue.StringValue -> NodeValue.STRING(json.toString())
              else -> NodeValue.STRING(json.serialise())
            }
            NodeValue.NULL -> NodeValue.STRING("")
            is NodeValue.SLIST -> value
            is NodeValue.STRING -> value
            is NodeValue.XML -> {
              when (val xml = value.xml) {
                is XmlValue.Attribute -> NodeValue.STRING("@${xml.name}='${escape(xml.value)}'")
                is XmlValue.Element -> NodeValue.STRING(renderXml(xml.element))
                is XmlValue.Text -> NodeValue.STRING(xml.text)
              }
            }
            else -> NodeValue.STRING(value.strForm())
          }
        }
      }
      is Result.Err -> return result.error
    }

    val result = if (values.size == 1) {
      values[0]
    } else {
      NodeValue.LIST(values)
    }

    return node.copy(result = NodeResult.VALUE(result),
      children = children.toMutableList())
  }

  private fun executeError(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    path: List<String>
  ): ExecutionPlanNode {
    val (children, values) = when (val result = evaluateChildren(valueResolver, node, path)) {
      is Result.Ok -> {
        result.value.first to result.value.second.flatMap { value ->
          when (val v = value.asValue().orDefault()) {
            is NodeValue.BARRAY -> listOf(v.strForm())
            is NodeValue.BOOL -> listOf(v.bool.toString())
            is NodeValue.JSON -> listOf(v.json.toString())
            is NodeValue.LIST -> v.items.map { item -> item.toString() }
            is NodeValue.MMAP -> listOf(v.strForm())
            is NodeValue.NAMESPACED -> listOf(v.strForm())
            is NodeValue.SLIST -> v.items
            is NodeValue.STRING -> listOf(v.string)
            is NodeValue.UINT -> listOf(v.uint.toString())
            else -> emptyList()
          }
        }
      }
      is Result.Err -> return result.error
    }

    return node.copy(result = NodeResult.ERROR(values.joinToString()),
      children = children.toMutableList())
  }

  private fun executeXmlParse(
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
              when (val result = handleWith<Node> { parse(argValue.bytes) }) {
                is Result.Ok -> if (result.value is Element) {
                  NodeResult.VALUE(NodeValue.XML(XmlValue.Element(result.value as Element)))
                } else {
                  NodeResult.ERROR("XML parse error: was expecting a root XML element")
                }
                is Result.Err -> NodeResult.ERROR("XML parse error: ${result.error.message}")
              }
            }
            NodeValue.NULL -> NodeResult.VALUE(NodeValue.NULL)
            is NodeValue.STRING -> {
              when (val result = handleWith<Node> { parse(argValue.string) }) {
                is Result.Ok -> if (result.value is Element) {
                  NodeResult.VALUE(NodeValue.XML(XmlValue.Element(result.value as Element)))
                } else {
                  NodeResult.ERROR("XML parse error: was expecting a root XML element")
                }
                is Result.Err -> NodeResult.ERROR("XML parse error: ${result.error.message}")
              }
            }
            else -> NodeResult.ERROR("xml:parse can not be used with ${argValue.valueType()}")
          }
        } else {
          NodeResult.VALUE(NodeValue.NULL)
        }
        node.copy(result = result, children = mutableListOf(resultNode.value))
      }
      is Result.Err -> node.copy(result = NodeResult.ERROR(resultNode.error))
    }
  }

  @Suppress("LongMethod", "CyclomaticComplexMethod")
  private fun executeExpectCount(
    action: String,
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    return when (val result = validateArgs(2, 1, node, action, valueResolver, actionPath)) {
      is Result.Ok -> {
        val expectedLength = result.value.first[0].value()
          .orDefault()
          .asValue()
          .orDefault()
          .asUInt()
          ?.toInt() ?: 0

        val second = result.value.first[1].value()
          .orDefault()
          .asValue()
          .orDefault()

        val error = when (second) {
          is NodeValue.JSON -> when (val json = second.json) {
            is JsonValue.Array -> {
              if (json.values.size != expectedLength) {
                "Expected $expectedLength array items but there were ${json.values.size}"
              } else {
                null
              }
            }
            is JsonValue.Object -> {
              if (json.entries.size != expectedLength) {
                "Expected $expectedLength object entries but there were ${json.entries.size}"
              } else {
                null
              }
            }
            is JsonValue.StringValue -> {
              if (json.size() != expectedLength) {
                "Expected a JSON string with length of $expectedLength but was ${second.json.size()}"
              } else {
                null
              }
            }
            else -> "'$action' can't be used with a $second node"
          }
          is NodeValue.LIST -> {
            if (second.items.size != expectedLength) {
              "Expected $expectedLength items but there were ${second.items.size}"
            } else {
              null
            }
          }
          is NodeValue.MMAP -> {
            if (second.entries.size != expectedLength) {
              "Expected $expectedLength map entries but there were ${second.entries.size}"
            } else {
              null
            }
          }
          is NodeValue.SLIST -> {
            if (second.items.size != expectedLength) {
              "Expected $expectedLength items but there were ${second.items.size}"
            } else {
              null
            }
          }
          is NodeValue.STRING -> {
            if (second.string.length != expectedLength) {
              "Expected a string with a length of $expectedLength but it was ${second.string.length}"
            } else {
              null
            }
          }
          is NodeValue.XML -> when (second.xml) {
            is XmlValue.Element -> {
              // If there is more than one child element, it will be stored as a List,
              // so will not end up here
              if (expectedLength > 1) {
                "Expected $expectedLength child elements but there were 1"
              } else {
                null
              }
            }
            else -> "'$action' can't be used with a $second node"
          }
          else -> "'$action' can't be used with a $second node"
        }

        if (error == null) {
          node.copy(result = NodeResult.OK, children = (result.value.first + result.value.second).toMutableList())
        } else {
          logger.debug { "expect:empty failed with an error: $error" }
          if (result.value.second.isNotEmpty()) {
            val errorNodeResult = walkTree(actionPath, result.value.second[0], valueResolver)
            when (val messageValue = errorNodeResult.value().orDefault()) {
              is NodeResult.VALUE -> {
                val message = messageValue.orDefault().asString() ?: ""
                node.copy(result = NodeResult.ERROR(message),
                  children = (result.value.first + errorNodeResult).toMutableList())
              }
              else -> {
                // There was an error generating the optional message, so just return the
                // original error
                node.copy(result = NodeResult.ERROR(error),
                  children = (result.value.first + errorNodeResult).toMutableList())
              }
            }
          } else {
            node.copy(result = NodeResult.ERROR(error),
              children = (result.value.first + result.value.second).toMutableList())
          }
        }
      }
      is Result.Err -> node.copy(result = NodeResult.ERROR(result.error))
    }
  }

  @Suppress("CyclomaticComplexMethod")
  private fun executeLength(
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
            is NodeValue.BARRAY -> NodeResult.VALUE(NodeValue.UINT(argValue.bytes.size.toUInt()))
            is NodeValue.JSON -> when (val json = argValue.json) {
              is JsonValue.Array -> NodeResult.VALUE(NodeValue.UINT(json.size().toUInt()))
              is JsonValue.Object -> NodeResult.VALUE(NodeValue.UINT(json.size().toUInt()))
              is JsonValue.StringValue -> NodeResult.VALUE(NodeValue.UINT(json.size().toUInt()))
              else -> NodeResult.ERROR("'length' can't be used with a ${resultNode.value} node")
            }
            is NodeValue.LIST -> NodeResult.VALUE(NodeValue.UINT(argValue.items.size.toUInt()))
            is NodeValue.MMAP -> NodeResult.VALUE(NodeValue.UINT(argValue.entries.size.toUInt()))
            NodeValue.NULL -> NodeResult.VALUE(NodeValue.UINT(0.toUInt()))
            is NodeValue.SLIST -> NodeResult.VALUE(NodeValue.UINT(argValue.items.size.toUInt()))
            is NodeValue.STRING -> NodeResult.VALUE(NodeValue.UINT(argValue.string.length.toUInt()))
            is NodeValue.XML -> when (val xml = argValue.xml) {
              is XmlValue.Attribute -> NodeResult.VALUE(NodeValue.UINT(1.toUInt()))
              is XmlValue.Element -> NodeResult.VALUE(NodeValue.UINT(1.toUInt()))
              is XmlValue.Text -> NodeResult.VALUE(NodeValue.UINT(xml.text.length.toUInt()))
            }
            else -> NodeResult.ERROR("'length' can't be used with a ${resultNode.value} node")
          }
        } else {
          NodeResult.VALUE(NodeValue.UINT(0.toUInt()))
        }
        node.copy(result = result, children = mutableListOf(resultNode.value))
      }
      is Result.Err -> node.copy(result = NodeResult.ERROR(resultNode.error))
    }
  }

  private fun executeXmlValue(
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
            is NodeValue.XML -> when (val xml = argValue.xml) {
              is XmlValue.Attribute -> NodeResult.VALUE(NodeValue.STRING(xml.value))
              else -> NodeResult.ERROR("'xml:value' can't be used with $xml")
            }
            else -> NodeResult.ERROR("'xml:value' can't be used with a ${argValue.valueType()} node")
          }
        } else {
          NodeResult.VALUE(NodeValue.NULL)
        }
        node.copy(result = result, children = mutableListOf(resultNode.value))
      }
      is Result.Err -> node.copy(result = NodeResult.ERROR(resultNode.error))
    }
  }

  private fun executeXmlAttributes(
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
            is NodeValue.XML -> when (val xml = argValue.xml) {
              is XmlValue.Attribute -> NodeResult.VALUE(
                NodeValue.ENTRY(xml.name, NodeValue.STRING(xml.value)))
              is XmlValue.Element -> NodeResult.VALUE(
                NodeValue.MMAP(attributes(xml.element).mapValues { listOf(it.value) }))
              else -> NodeResult.ERROR("'xml:attributes' can't be used with $xml")
            }
            else -> NodeResult.ERROR("'xml:attributes' can't be used with a ${argValue.valueType()} node")
          }
        } else {
          NodeResult.VALUE(NodeValue.NULL)
        }
        node.copy(result = result, children = mutableListOf(resultNode.value))
      }
      is Result.Err -> node.copy(result = NodeResult.ERROR(resultNode.error))
    }
  }

  private fun executeForEach(
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    actionPath: List<String>
  ): ExecutionPlanNode {
    return when (val argResult = validateArgs(2, 1, node, "for-each", valueResolver, actionPath)) {
      is Result.Ok -> {
        val (markerResult, loopItems) = argResult.value.first
        val marker = markerResult.value().orDefault().asString() ?: "[*]"
        when (loopItems.result) {
          is NodeResult.ERROR -> node.copy(result = loopItems.result,
            children = mutableListOf(markerResult, loopItems, argResult.value.second[0]))
          else -> {
            var result: NodeResult = NodeResult.OK
            val childResults = mutableListOf(markerResult, loopItems)

            val loopItems = loopItems.result
              .orDefault()
              .asValue()
              .orDefault()
              .toList()
            for (index in 0..<loopItems.size) {
              val updatedChild = injectIndex(argResult.value.second[0], marker, index)
              val childResult = walkTree(actionPath, updatedChild, valueResolver)
              result = result.and(childResult.result)
              childResults.add(childResult)
            }

            node.copy(result = result.truthy(), children = childResults)
          }
        }
      }
      is Result.Err -> node.copy(result = NodeResult.ERROR(argResult.error))
    }
  }

  private fun executeAnd(
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    path: List<String>
  ): ExecutionPlanNode {
    val (children, result) = when (val result = evaluateChildren(valueResolver, node, path)) {
      is Result.Ok -> {
        result.value.first to result.value.second.fold(NodeResult.OK as NodeResult) { acc, value ->
          acc.and(value)
        }
      }
      is Result.Err -> return result.error
    }

    return node.copy(result = result, children = children.toMutableList())
  }

  private fun executeOr(
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    path: List<String>
  ): ExecutionPlanNode {
    val (children, result) = when (val result = evaluateChildren(valueResolver, node, path, false)) {
      is Result.Ok -> {
        result.value.first to result.value.second.fold(NodeResult.OK as NodeResult) { acc, value ->
          acc.or(value)
        }
      }
      is Result.Err -> return result.error
    }

    return node.copy(result = result, children = children.toMutableList())
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

  @Suppress("LongMethod", "CyclomaticComplexMethod", "NestedBlockDepth")
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

        is NodeValue.XML -> {
          if (path.isRoot()) {
            Result.Ok(NodeValue.XML(result.value.xml))
          } else {
            val element = result.value.xml.asElement()
            if (element != null) {
              val xmlPaths = resolvePath(element, path)
              logger.trace { "resolved path $path -> $xmlPaths" }
              if (xmlPaths.isEmpty()) {
                Result.Ok(NodeValue.NULL)
              } else if (xmlPaths.size == 1) {
                val value = resolveMatchingNode(element, xmlPaths[0])
                if (value != null) {
                  Result.Ok(NodeValue.XML(XmlValue.fromResult(value)))
                } else {
                  Result.Ok(NodeValue.NULL)
                }
              } else {
                val values = xmlPaths
                  .map { path ->
                    val result = resolveMatchingNode(element, path)
                    if (result != null) {
                      NodeValue.XML(XmlValue.fromResult(result))
                    } else {
                      NodeValue.NULL
                    }
                  }
                Result.Ok(NodeValue.LIST(values))
              }
            } else {
              Result.Err("Can not resolve '$path', current stack value does not contain a value that is " +
                "resolvable (${result.value})")
            }
          }
        }

        else -> Result.Err("Can not resolve '$path', current stack value does not contain a value that is " +
          "resolvable (${result.value})")
      }

      else -> Result.Err("Can not resolve '$path', current stack value does not contain a value")
    }
  }

  private fun evaluateChildren(
    valueResolver: ValueResolver,
    node: ExecutionPlanNode,
    path: List<String>,
    shortCircuit: Boolean = true
  ): Result<Pair<List<ExecutionPlanNode>, List<NodeResult>>, ExecutionPlanNode> {
    val children = mutableListOf<ExecutionPlanNode>()
    val values = mutableListOf<NodeResult>()
    val loopItems = ArrayDeque(node.children)

    while (loopItems.isNotEmpty()) {
      val child = loopItems.removeFirst()

      var nodeResult = child.value()
      if (nodeResult == null) {
        val result = walkTree(path, child, valueResolver)
        if (result.result is NodeResult.ERROR && shortCircuit) {
          children.add(result)
          children.addAll(loopItems)
          return Result.Err(node.copy(result = result.result, children = children))
        }
        else if (result.isSplat()) {
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

      values.add(nodeResult)
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
          "The following expected entries were missing: ${diff.joinToString(", ")}" to diff
        }
      }
      "expect:only-entries" -> {
        val diff = actualKeys - expectedKeys
        if (diff.isEmpty()) {
          null
        } else {
          "The following unexpected entries were received: ${diff.joinToString(", ")}" to diff
        }
      }
      else -> "'$action' is not a valid action" to null
    }
  }

  private fun injectIndex(
    node: ExecutionPlanNode,
    marker: String,
    index: Int
  ): ExecutionPlanNode {
    return when (node.nodeType) {
      is PlanNodeType.ACTION -> node.copy(children = node.children
        .map { injectIndex(it, marker, index) }
        .toMutableList())
      is PlanNodeType.CONTAINER -> {
        when (val pathResult = handleWith<DocPath> { DocPath(node.nodeType.label) }) {
          is Result.Ok -> node.copy(
            nodeType = PlanNodeType.CONTAINER(injectIndexInPath(pathResult.value, marker, index).toString()),
            children = node.children
              .map { injectIndex(it, marker, index) }
              .toMutableList())
          is Result.Err -> node.copy(children = node.children
            .map { injectIndex(it, marker, index) }
            .toMutableList())
        }
      }
      is PlanNodeType.RESOLVE_CURRENT -> node.copy(nodeType = PlanNodeType.RESOLVE_CURRENT(
        injectIndexInPath(node.nodeType.path, marker, index)
      ))
      PlanNodeType.SPLAT, PlanNodeType.PIPELINE -> node.copy(children = node.children
        .map { injectIndex(it, marker, index) }
        .toMutableList())
      else -> node
    }
  }

  private fun injectIndexInPath(
    path: DocPath,
    marker: String,
    index: Int
  ): DocPath {
    return DocPath(path.tokens().flatMap {
      when (it) {
        is PathToken.Field -> if (it.name == marker) {
          listOf(PathToken.Field(marker.dropLast(1)), PathToken.Index(index))
        } else listOf(it)
        else -> listOf(it)
      }
    })
  }
}
