package au.com.dius.pact.core.matchers.engine.bodies

import au.com.dius.pact.core.matchers.QualifiedName
import au.com.dius.pact.core.matchers.engine.ExecutionPlanNode
import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.NodeValue.Companion.escape
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
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
    val elementPath = if (path.endsWith("$name[*]")) {
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

      val noIndices = dropIndices(p)
      //      let matchers = context.select_best_matcher(&p)
      //        .and_rules(&context.select_best_matcher(&no_indices))
      //        .remove_duplicates();
      //      if !matchers.is_empty() {
      //        item_node.add(ExecutionPlanNode::annotation(format!("@{} {}", key, matchers.generate_description(true))));
      //        presence_check.add(build_matching_rule_node(&ExecutionPlanNode::value_node(item_value),
      //          ExecutionPlanNode::action("xml:value")
      //            .add(ExecutionPlanNode::resolve_current_value(&p)),
      //          &matchers, false));
      //      } else {
          itemNode.add(ExecutionPlanNode.annotation(Into { "@${key}=${escape(value.nodeValue)}" }))
          val itemCheck = ExecutionPlanNode.action("match:equality")
          itemCheck
            .add(ExecutionPlanNode.valueNode(itemValue))
            .add(ExecutionPlanNode.action("xml:value")
              .add(ExecutionPlanNode.resolveCurrentValue(p)))
            .add(ExecutionPlanNode.valueNode(NodeValue.NULL))
          presenceCheck.add(itemCheck)
      //      }

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
        );
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

    //      if (!context.type_matcher_defined(&p)) {
        parentNode.add(
          ExecutionPlanNode.action("expect:count")
            .add(ExecutionPlanNode.valueNode(NodeValue.UINT(elements.size.toUInt())))
            .add(ExecutionPlanNode.resolveCurrentValue(p))
            .add(
              ExecutionPlanNode.action("join")
                .add(ExecutionPlanNode.valueNode(
                  "Expected ${elements.size} <${childName}> child element${if (elements.size > 1) "s" else ""} but there were "))
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
    //      } else {
    //        let rules = context.select_best_matcher(&p)
    //          .filter(|m| m.is_length_type_matcher());
    //        if !rules.is_empty() {
    //          parent_node.add(ExecutionPlanNode::annotation(format!("{} {}",
    //            path.last_field().unwrap_or_default(),
    //            rules.generate_description(true))));
    //          parent_node.add(build_matching_rule_node(&ExecutionPlanNode::value_node(elements[0]),
    //            &ExecutionPlanNode::resolve_current_value(path), &rules, true));
    //        }

//        val forEachNode = ExecutionPlanNode.action("for-each")
//        forEachNode.add(ExecutionPlanNode.resolveCurrentValue(p))
//        val itemPath = p.join("[*]")
//
//        processElement(context, elements[0], 0, itemPath, forEachNode)
//
//        parentNode.add(forEachNode)
    //      }
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
    //    let matchers = context.select_best_matcher(&p)
    //      .filter(|matcher| !matcher.is_type_matcher())
    //      .and_rules(&context.select_best_matcher(&no_indices)
    //        .filter(|matcher| !matcher.is_type_matcher())
    //      ).remove_duplicates();
    //    if !matchers.is_empty() {
    //      node.add(ExecutionPlanNode::annotation(format!("{} {}", p.last_field().unwrap_or_default(),
    //        matchers.generate_description(false))));
    //      let mut current_value = ExecutionPlanNode::action("to-string");
    //      current_value.add(ExecutionPlanNode::resolve_current_value(&p));
    //      node.add(build_matching_rule_node(&ExecutionPlanNode::value_node(text_nodes.join("")),
    //        &current_value, &matchers, false));
    //    } else {
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
    //    }
  }

  private fun textContent(element: Node): String {
    val childNodes = element.childNodes
    return (0..<childNodes.length)
      .map { index -> childNodes.item(index) }
      .filter { it.nodeType == Node.TEXT_NODE }
      .joinToString("") { it.textContent }
  }

  private fun dropIndices(path: DocPath) = DocPath(path.pathTokens.filter {
    it !is PathToken.Index && it !is PathToken.StarIndex
  })
}
