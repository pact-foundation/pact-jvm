package au.com.dius.pact.core.matchers.engine.bodies

import au.com.dius.pact.core.matchers.QualifiedName
import au.com.dius.pact.core.matchers.engine.ExecutionPlanNode
import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.NodeValue.Companion.escape
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.matchers.engine.buildMatchingRuleNode
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.Into
import au.com.dius.pact.core.model.PathToken
import au.com.dius.pact.core.model.XmlUtils.groupChildren
import au.com.dius.pact.core.model.XmlUtils.parse
import au.com.dius.pact.core.support.getOr
import io.github.oshai.kotlinlogging.KotlinLogging
import org.w3c.dom.Node

private val logger = KotlinLogging.logger {}

/**
 * Plan builder for XML bodies
 */
object XMLPlanBuilder: PlanBodyBuilder  {
  override fun supportsType(contentType: ContentType) = contentType.isXml()

  override fun buildPlan(
    content: ByteArray,
    context: PlanMatchingContext
  ): ExecutionPlanNode {
    val rootElement = parse(content)

    val bodyNode = ExecutionPlanNode.action("tee")
    bodyNode
      .add(ExecutionPlanNode.action("xml:parse")
        .add(ExecutionPlanNode.resolveValue(DocPath("$.body"))))

    val path = DocPath.root()
    val rootNode = ExecutionPlanNode.container(path.toString())
    processElement(context, rootElement, null, path, rootNode)
    bodyNode.add(rootNode)

    return bodyNode
  }

  private fun processElement(
    context: PlanMatchingContext,
    element: Node,
    index: Int?,
    path: DocPath,
    node: ExecutionPlanNode
  ) {
    val name = QualifiedName(element)
    val elementPath = if (path.endsWith("['${name}*']")) {
      path
    } else if (index != null) {
      path.pushField(name.toString()).pushIndex(index)
    } else {
      path.pushField(name.toString())
    }

    val presenceCheck = ExecutionPlanNode.action("if")
    presenceCheck
      .add(ExecutionPlanNode.action("check:exists")
        .add(ExecutionPlanNode.resolveCurrentValue(elementPath)))
    val itemNode = ExecutionPlanNode.container(elementPath.toString())

    if (element.hasAttributes()) {
      val attributesNode = ExecutionPlanNode.container("attributes")
      processAttributes(elementPath, element, attributesNode, context)
      itemNode.add(attributesNode)
    }

    val textNode = ExecutionPlanNode.container("#text")
    processText(elementPath, element, textNode, context)
    itemNode.add(textNode)

    processChildren(context, elementPath, element, itemNode)
    presenceCheck.add(itemNode)

    val errorNode = ExecutionPlanNode.action("error")
    errorNode.add(
      ExecutionPlanNode.valueNode(
        "Was expecting an XML element ${elementPath.asJsonPointer().getOr(name.toString()) } but it was missing")
    )
    presenceCheck.add(errorNode)

    node.add(presenceCheck)
  }

  @Suppress("LongMethod")
  private fun processAttributes(
    path: DocPath,
    element: Node,
    node: ExecutionPlanNode,
    context: PlanMatchingContext
  ) {
    val attributes = (0..<element.attributes.length)
      .map { element.attributes.item(it) }
    val keys = attributes.map { it.nodeName }.sorted()
    for (key in keys) {
      val p = path.pushField("@${key}")
      val value = element.attributes.getNamedItem(key)
      val itemNode = ExecutionPlanNode.container(p.toString())

      val presenceCheck = ExecutionPlanNode.action("if")
      val itemValue = NodeValue.STRING(value.nodeValue)
      presenceCheck
        .add(
          ExecutionPlanNode.action("check:exists")
            .add(ExecutionPlanNode.resolveCurrentValue(p))
        )

      val noMarkers = p.dropMarkers()
      val noIndices = dropIndices(noMarkers)
      val matchers = context.selectBestMatcher(noMarkers, noIndices)
      if (matchers.isNotEmpty()) {
        itemNode.add(ExecutionPlanNode.annotation(Into { "@${key} ${matchers.generateDescription(true)}" }))
        presenceCheck.add(buildMatchingRuleNode(ExecutionPlanNode.valueNode(itemValue),
          ExecutionPlanNode.action("xml:value")
            .add(ExecutionPlanNode.resolveCurrentValue(p)),
          matchers, false))
      } else {
          itemNode.add(ExecutionPlanNode.annotation(Into { "@${key}=${escape(value.nodeValue)}" }))
          val itemCheck = ExecutionPlanNode.action("match:equality")
          itemCheck
            .add(ExecutionPlanNode.valueNode(itemValue))
            .add(ExecutionPlanNode.action("xml:value")
              .add(ExecutionPlanNode.resolveCurrentValue(p)))
            .add(ExecutionPlanNode.valueNode(NodeValue.NULL))
          presenceCheck.add(itemCheck)
      }

      itemNode.add(presenceCheck)
      node.add(itemNode)
    }

    node.add(
      ExecutionPlanNode.action("expect:entries")
        .add(ExecutionPlanNode.valueNode(NodeValue.SLIST(keys)))
        .add(ExecutionPlanNode.action("xml:attributes")
          .add(ExecutionPlanNode.resolveCurrentValue(path)))
        .add(
          ExecutionPlanNode.action("join")
            .add(ExecutionPlanNode.valueNode("The following expected attributes were missing: "))
            .add(ExecutionPlanNode.action("join-with")
              .add(ExecutionPlanNode.valueNode(", "))
              .add(
                ExecutionPlanNode.splat()
                  .add(ExecutionPlanNode.action("apply"))
              )
            )
        )
    )

    if (!context.config.allowUnexpectedEntries) {
      node.add(
        ExecutionPlanNode.action("expect:only-entries")
          .add(ExecutionPlanNode.valueNode(NodeValue.SLIST(keys)))
          .add(ExecutionPlanNode.action("xml:attributes")
            .add(ExecutionPlanNode.resolveCurrentValue(path)))
      )
    }
  }

  @Suppress("LongMethod")
  private fun processChildren(
    context: PlanMatchingContext,
    path: DocPath,
    element: Node,
    parentNode: ExecutionPlanNode
  ) {
    val children = groupChildren(element)

    if (!context.config.allowUnexpectedEntries) {
      if (children.isEmpty()) {
        parentNode.add(
          ExecutionPlanNode.action("expect:empty")
            .add(ExecutionPlanNode.resolveCurrentValue(path))
        )
      } else {
        parentNode.add(
          ExecutionPlanNode.action("expect:only-entries")
            .add(ExecutionPlanNode.valueNode(Into { NodeValue.SLIST(children.keys.toList()) }))
            .add(ExecutionPlanNode.resolveCurrentValue(path))
        )
      }
    }

    for ((childName, elements) in children) {
      val p = path.join(childName)

      val noIndices = dropIndices(p)
      val matchers = context.selectBestMatcher(p)
        .andRules(context.selectBestMatcher(noIndices))
        .filter { matcher -> matcher.isTypeMatcher() }
        .removeDuplicates()
      if (matchers.isEmpty()) {
        parentNode.add(
          ExecutionPlanNode.action("expect:count")
            .add(ExecutionPlanNode.valueNode(NodeValue.UINT(elements.size.toUInt())))
            .add(ExecutionPlanNode.resolveCurrentValue(p))
            .add(
              ExecutionPlanNode.action("join")
                .add(ExecutionPlanNode.valueNode(
                  "Expected ${elements.size} <${childName}> child element${if (elements.size > 1) "s" else ""}" +
                    " but there were "))
                .add(ExecutionPlanNode.action("length")
                  .add(ExecutionPlanNode.resolveCurrentValue(p)))
            )
        )

        if (elements.size == 1) {
          processElement(context, elements[0], 0, path, parentNode)
        } else {
          for ((index, child) in elements.withIndex()) {
            processElement(context, child, index, path, parentNode)
          }
        }
      } else {
        val rules = matchers.filter { it.isLengthTypeMatcher() }
        if (rules.isNotEmpty()) {
          parentNode.add(ExecutionPlanNode.annotation(Into {
            "${p.lastField()} ${rules.generateDescription(true)}"
          }))
          parentNode.add(buildMatchingRuleNode(ExecutionPlanNode.valueNode(elements[0]),
            ExecutionPlanNode.resolveCurrentValue(p), rules, true))
        }

        val forEachNode = ExecutionPlanNode.action("for-each")
        forEachNode.add(ExecutionPlanNode.valueNode("$childName*"))
        forEachNode.add(ExecutionPlanNode.resolveCurrentValue(p))
        val itemPath = path.join("$childName*")

        processElement(context, elements[0], 0, itemPath, forEachNode)

        parentNode.add(forEachNode)
      }
    }
  }

  private fun processText(
    path: DocPath,
    element: Node,
    node: ExecutionPlanNode,
    context: PlanMatchingContext
  ) {
    val text = textContent(element)
    val p = path.join("#text")
    val noIndices = dropIndices(p)
    val matchers = context.selectBestMatcher(p)
      .filter { matcher -> !matcher.isTypeMatcher() }
      .andRules(context.selectBestMatcher(noIndices)
        .filter { matcher -> !matcher.isTypeMatcher() }
      ).removeDuplicates()
    if (matchers.isNotEmpty()) {
      node.add(ExecutionPlanNode.annotation(Into { "${p.lastField()} ${matchers.generateDescription(false)}" }))
      val currentValue = ExecutionPlanNode.action("to-string")
      currentValue.add(ExecutionPlanNode.resolveCurrentValue(p))
      node.add(buildMatchingRuleNode(ExecutionPlanNode.valueNode(text),
        currentValue, matchers, false))
    } else {
      if (text.isNotEmpty() && text.isNotBlank()) {
        val matchNode = ExecutionPlanNode.action("match:equality")
        matchNode
          .add(ExecutionPlanNode.valueNode(NodeValue.STRING(text)))
          .add(ExecutionPlanNode.action("to-string")
            .add(ExecutionPlanNode.resolveCurrentValue(p)))
          .add(ExecutionPlanNode.valueNode(NodeValue.NULL))
        node.add(matchNode)
      } else {
        node.add(ExecutionPlanNode.action("expect:empty")
          .add(ExecutionPlanNode.action("to-string")
            .add(ExecutionPlanNode.resolveCurrentValue(p))))
      }
    }
  }

  private fun textContent(element: Node): String {
    val childNodes = element.childNodes
    return (0..<childNodes.length)
      .map { index -> childNodes.item(index) }
      .filter { it.nodeType == Node.TEXT_NODE }
      .joinToString("") { it.textContent }
  }

  private fun dropIndices(path: DocPath) = DocPath(path.pathTokens
    .filter {
      it !is PathToken.Index && it !is PathToken.StarIndex
    }
  )
}
