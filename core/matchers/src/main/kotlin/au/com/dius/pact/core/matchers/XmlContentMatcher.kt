package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.support.zipAll
import mu.KLogging
import org.apache.xerces.dom.TextImpl
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.Node.CDATA_SECTION_NODE
import org.w3c.dom.Node.ELEMENT_NODE
import org.w3c.dom.Node.TEXT_NODE
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

object XmlContentMatcher : ContentMatcher, KLogging() {

  override fun matchBody(
    expected: OptionalBody,
    actual: OptionalBody,
    context: MatchingContext
  ): BodyMatchResult {
    return when {
      expected.isMissing() -> BodyMatchResult(null, emptyList())
      expected.isEmpty() && actual.isEmpty() -> BodyMatchResult(null, emptyList())
      actual.isMissing() ->
        BodyMatchResult(null,
          listOf(BodyItemMatchResult("$", listOf(
            BodyMismatch(expected.unwrap(), null, "Expected body '${expected.value}' but was missing")))))
      else -> {
        BodyMatchResult(null, compareNode(listOf("$"), parse(expected.valueAsString()), parse(actual.valueAsString()),
          context))
      }
    }
  }

  override fun setupBodyFromConfig(
    bodyConfig: Map<String, Any?>
  ): Triple<OptionalBody, MatchingRuleCategory?, Generators?> {
    return Triple(
      OptionalBody.body(
        bodyConfig["body"].toString().toByteArray(),
        ContentType("application/xml")
      ),
      null,
      null
    )
  }

  fun parse(xmlData: String): Node {
    return if (xmlData.isEmpty()) {
      TextImpl()
    } else {
      val dbFactory = DocumentBuilderFactory.newInstance()
      if (System.getProperty("pact.matching.xml.validating") == "false") {
        dbFactory.isValidating = false
        dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
        dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
      }
      if (System.getProperty("pact.matching.xml.namespace-aware") != "false") {
        dbFactory.isNamespaceAware = true
      }
      val dBuilder = dbFactory.newDocumentBuilder()
      val xmlInput = InputSource(StringReader(xmlData))
      val doc = dBuilder.parse(xmlInput)
      doc.documentElement
    }
  }

  private fun appendAttribute(path: List<String>, attribute: QualifiedName): List<String> {
    return path + "@${attribute.nodeName}"
  }

  fun compareText(
    path: List<String>,
    expected: Node,
    actual: Node,
    context: MatchingContext
  ): List<BodyItemMatchResult> {
    val textpath = path + "#text"
    val expectedText = asList(expected.childNodes).filter { n ->
      n.nodeType == TEXT_NODE || n.nodeType == CDATA_SECTION_NODE
    }.joinToString("") { n -> n.textContent.trim() }
    val actualText = asList(actual.childNodes).filter { n ->
      n.nodeType == TEXT_NODE || n.nodeType == CDATA_SECTION_NODE
    }.joinToString("") { n -> n.textContent.trim() }
    return when {
      context.matcherDefined(textpath) -> {
        logger.debug { "compareText: Matcher defined for path $textpath" }
        listOf(BodyItemMatchResult(path.joinToString("."),
          Matchers.domatch(context, textpath, expectedText, actualText, BodyMismatchFactory)))
      }
      expectedText != actualText ->
        listOf(BodyItemMatchResult(path.joinToString("."),
          listOf(BodyMismatch(expected, actual, "Expected value '$expectedText' but received '$actualText'",
          textpath.joinToString(".")))))
      else -> listOf(BodyItemMatchResult(path.joinToString("."), emptyList()))
    }
  }

  private fun asList(childNodes: NodeList?): List<Node> {
    return if (childNodes == null) {
      emptyList()
    } else {
      val list = mutableListOf<Node>()
      for (i in 0 until childNodes.length) {
        list.add(childNodes.item(i))
      }
      list
    }
  }

  private fun compareNode(
    path: List<String>,
    expected: Node,
    actual: Node,
    context: MatchingContext
  ): List<BodyItemMatchResult> {
    val nodePath = path + expected.nodeName
    val mismatches = when {
      context.matcherDefined(nodePath) -> {
        logger.debug { "compareNode: Matcher defined for path $nodePath" }
        listOf(BodyItemMatchResult(path.joinToString("."),
          Matchers.domatch(context, nodePath, expected, actual, BodyMismatchFactory)))
      }
      else -> {
        val actualName = QualifiedName(actual)
        val expectedName = QualifiedName(expected)

        when {
          actualName != expectedName -> listOf(BodyItemMatchResult(path.joinToString("."),
            listOf(BodyMismatch(expected, actual,
                  "Expected element $expectedName but received $actualName",
                  nodePath.joinToString(".")))))
          else -> listOf(BodyItemMatchResult(path.joinToString("."), emptyList()))
        }
      }
    }

    return if (mismatches.all { it.result.isEmpty() }) {
      compareAttributes(nodePath, expected, actual, context) +
        compareChildren(nodePath, expected, actual, context) +
        compareText(nodePath, expected, actual, context)
    } else {
      mismatches
    }
  }

  private fun compareChildren(
    path: List<String>,
    expected: Node,
    actual: Node,
    context: MatchingContext
  ): List<BodyItemMatchResult> {
    val expectedChildren = asList(expected.childNodes).filter { n -> n.nodeType == ELEMENT_NODE }
    val actualChildren = asList(actual.childNodes).filter { n -> n.nodeType == ELEMENT_NODE }
    val mismatches = mutableListOf<BodyItemMatchResult>()
    val key = path.joinToString(".")
    if (expectedChildren.isEmpty() && actualChildren.isNotEmpty() && !context.allowUnexpectedKeys) {
      mismatches.add(BodyItemMatchResult(key, listOf(BodyMismatch(expected, actual,
          "Expected an empty List but received ${actualChildren.size} child nodes",
        key))))
    }

    val expectedChildrenByQName = expectedChildren.groupBy { QualifiedName(it) }.toMutableMap()
    mismatches.addAll(actualChildren
      .groupBy { QualifiedName(it) }
      .flatMap { e ->
        val childPath = path + e.key.toString()
        if (expectedChildrenByQName.contains(e.key)) {
          val expectedChild = expectedChildrenByQName.remove(e.key)!!
          if (context.matcherDefined(childPath)) {
            val list = mutableListOf<BodyItemMatchResult>()
            logger.debug { "compareChild: Matcher defined for path $childPath" }
            e.value.forEach { actualChild ->
              list.add(BodyItemMatchResult(childPath.joinToString("."),
                Matchers.domatch(context, childPath, actualChild, expectedChild.first(), BodyMismatchFactory)))
              list.addAll(compareNode(path, expectedChild.first(), actualChild, context))
            }
            list
          } else {
            expectedChild.zipAll(e.value).mapIndexed { index, comp ->
              val expectedNode = comp.first
              val actualNode = comp.second
              when {
                expectedNode == null -> if (context.allowUnexpectedKeys || actualNode == null) {
                  listOf(BodyItemMatchResult(key, emptyList()))
                } else {
                  listOf(BodyItemMatchResult(key, listOf(BodyMismatch(expected, actual,
                    "Unexpected child <${e.key}/>",
                    (path + actualNode.nodeName + index.toString()).joinToString(".")))))
                }
                actualNode == null -> listOf(BodyItemMatchResult(key,
                  listOf(BodyMismatch(expected, actual,
                  "Expected child <${e.key}/> but was missing",
                  (path + expectedNode.nodeName + index.toString()).joinToString(".")))))
                else -> compareNode(path, expectedNode, actualNode, context)
              }
            }.flatten()
          }
        } else if (!context.allowUnexpectedKeys || context.typeMatcherDefined(childPath)) {
          listOf(BodyItemMatchResult(key, listOf(BodyMismatch(expected, actual,
            "Unexpected child <${e.key}/>", key))))
        } else {
          listOf(BodyItemMatchResult(key, emptyList()))
        }
      })
    if (expectedChildrenByQName.isNotEmpty()) {
      expectedChildrenByQName.keys.forEach {
        mismatches.add(BodyItemMatchResult(key, listOf(BodyMismatch(expected, actual,
          "Expected child <$it/> but was missing", key))))
      }
    }
    return mismatches
  }

  private fun compareAttributes(
    path: List<String>,
    expected: Node,
    actual: Node,
    context: MatchingContext
  ): List<BodyItemMatchResult> {
    val expectedAttrs = attributesToMap(expected.attributes)
    val actualAttrs = attributesToMap(actual.attributes)

    return if (expectedAttrs.isEmpty() && actualAttrs.isNotEmpty() && !context.allowUnexpectedKeys) {
      listOf(BodyItemMatchResult(path.joinToString("."), listOf(BodyMismatch(expected, actual,
        "Expected a Tag with at least ${expectedAttrs.size} attributes but " +
          "received ${actual.attributes.length} attributes",
        path.joinToString("."), generateAttrDiff(expected, actual)))))
    } else {
      val mismatches = if (expectedAttrs.size > actualAttrs.size) {
        listOf(BodyMismatch(expected, actual, "Expected a Tag with at least ${expected.attributes.length} " +
          "attributes but received ${actual.attributes.length} attributes",
          path.joinToString("."), generateAttrDiff(expected, actual)))
      } else if (!context.allowUnexpectedKeys && expectedAttrs.size != actualAttrs.size) {
        listOf(BodyMismatch(expected, actual, "Expected a Tag with ${expected.attributes.length} " +
          "attributes but received ${actual.attributes.length} attributes",
          path.joinToString("."), generateAttrDiff(expected, actual)))
      } else {
        emptyList()
      }

      listOf(BodyItemMatchResult(path.joinToString("."), mismatches + expectedAttrs.flatMap { attr ->
        if (actualAttrs.contains(attr.key)) {
          val attrPath = appendAttribute(path, attr.key)
          val actualVal = actualAttrs[attr.key]
          when {
            context.matcherDefined(attrPath) -> {
              logger.debug { "compareText: Matcher defined for path $attrPath" }
              Matchers.domatch(context, attrPath, attr.value, actualVal, BodyMismatchFactory)
            }
            attr.value.nodeValue != actualVal?.nodeValue ->
              listOf(BodyMismatch(expected, actual, "Expected ${attr.key}='${attr.value.nodeValue}' " +
                "but received ${attr.key}='${actualVal?.nodeValue}'",
                attrPath.joinToString("."), generateAttrDiff(expected, actual)))
            else -> emptyList()
          }
        } else {
          listOf(BodyMismatch(expected, actual, "Expected ${attr.key}='${attr.value.nodeValue}' " +
            "but was missing",
            appendAttribute(path, attr.key).joinToString("."), generateAttrDiff(expected, actual)))
        }
      }))
    }
  }

  private fun generateAttrDiff(expected: Node, actual: Node): String {
    val expectedXml = generateXMLForNode(expected)
    val actualXml = generateXMLForNode(actual)
    return generateDiff(expectedXml, actualXml).joinToString(separator = "\n")
  }

  private fun generateXMLForNode(node: Node): String {
    return if (node.hasAttributes()) {
      val attr = attributesToMap(node.attributes).entries.sortedBy { it.key.toString() }.joinToString {
        "  @${it.key}=\"${it.value.nodeValue}\"\n"
      }
      "<${node.nodeName}\n$attr>"
    } else {
      "<${node.nodeName}>"
    }
  }

  private fun attributesToMap(attributes: NamedNodeMap?): Map<QualifiedName, Node> {
    return if (attributes == null) {
      emptyMap()
    } else {
      (0 until attributes.length)
              .map { attributes.item(it) }
              .filter { it.namespaceURI != XMLConstants.XMLNS_ATTRIBUTE_NS_URI }
              .map { QualifiedName(it) to it }
              .toMap()
    }
  }
}
