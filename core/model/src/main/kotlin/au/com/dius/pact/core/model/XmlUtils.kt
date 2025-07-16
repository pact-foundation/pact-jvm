package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.getOr
import au.com.dius.pact.core.support.handleWith
import io.github.oshai.kotlinlogging.KotlinLogging
import org.w3c.dom.Attr
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private val logger = KotlinLogging.logger {}

data class TreeNode(
  val id: String,
  var parent: TreeNode?,
  val children: MutableList<TreeNode> = mutableListOf()
) {
  fun <T> depthFirst(callback: (TreeNode) -> T) {
    if (children.isEmpty()) {
      callback(this)
    } else {
      for (child in children) {
        child.depthFirst(callback)
      }
    }
  }

  fun <T> ancestors(callback: (TreeNode) -> T): List<T> {
    val list = mutableListOf(callback(this))
    var next = parent
    while (next != null) {
      list.add(callback(next))
      next = next.parent
    }
    return list
  }

  fun addChild(childId: String): TreeNode {
    val child = TreeNode(childId, this)
    children.add(child)
    return child
  }

  fun detach() {
    parent?.removeChild(this)
    parent = null
  }

  fun removeChild(node: TreeNode) {
    children.remove(node)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as TreeNode

    return id == other.id
  }

  override fun hashCode() = id.hashCode()
}

/**
 * Enum to box the result value from resolveMatchingNode
 */
sealed class XmlResult {
  /** Matched XML element */
  data class ElementNode(val element: Element): XmlResult()

  /** Matched XML text */
  data class TextNode(val text: String): XmlResult()

  /** Matches an attribute */
  data class Attribute(val name: String, val value: String): XmlResult()
}

/**
 * Utility functions for XML
 */
object XmlUtils {
  /**
   * Parse a string into an XML Node. This will use the following boolean system properties:
   * - `pact.matching.xml.validating` to enable XML schema validation
   * - `pact.matching.xml.namespace-aware` to enable support for XML namespaces
   */
  fun parse(xmlData: String) = parseXml(InputSource(StringReader(xmlData)))

  /**
   * Parse a string into an XML Node. This will use the following boolean system properties:
   * - `pact.matching.xml.validating` to enable XML schema validation
   * - `pact.matching.xml.namespace-aware` to enable support for XML namespaces
   */
  fun parse(xmlData: ByteArray) = parseXml(InputSource(ByteArrayInputStream(xmlData)))

  fun parseXml(data: InputSource): Node {
    val dbFactory = DocumentBuilderFactory.newInstance()
    if (System.getProperty("pact.matching.xml.validating") == "false") {
      dbFactory.isValidating = false
      dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
      dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    }
    if (System.getProperty("pact.matching.xml.namespace-aware") != "false") {
      dbFactory.isNamespaceAware = true
    }
    return if (data.isEmpty) {
      dbFactory.newDocumentBuilder().newDocument()
    } else {
      val dBuilder = dbFactory.newDocumentBuilder()
      val doc = dBuilder.parse(data)
      doc.documentElement
    }
  }

  /** Render a XML node as a XML fragment */
  fun renderXml(node: Node): String {
    val t = TransformerFactory.newInstance().newTransformer()
    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    t.setOutputProperty(OutputKeys.INDENT, "yes")
    val buffer = StringWriter()
    t.transform(DOMSource(node), StreamResult(buffer))
    return buffer.toString().trim()
  }

  /** Group the child nodes by tag name */
  fun groupChildren(element: Node): Map<String, MutableList<Element>> {
    val childNodes = element.childNodes
    return (0..<childNodes.length)
      .map { index -> childNodes.item(index) }
      .filter { it.nodeType == Node.ELEMENT_NODE }
      .fold(mutableMapOf()) { acc, node ->
        val tagName = node.nodeName
        if (acc.contains(tagName)) {
          acc[tagName]!!.add(node as Element)
        } else {
          acc[tagName] = mutableListOf(node as Element)
        }
        acc
      }
  }

  /** Returns the child elements of the node */
  fun childElements(element: Node): List<Element> {
    return (0..<element.childNodes.length)
      .map { index -> element.childNodes.item(index) }
      .filter { it.nodeType == Node.ELEMENT_NODE } as List<Element>
  }

  /** Returns a Map of all the node's attributes */
  fun attributes(node: Element): Map<String, String> {
    val attr = node.attributes
    return (0..<attr.length)
      .map { index -> attr.item(index) as Attr }
      .associate { it.name to it.value }
  }

  /** Resolve the path expression against the XML, returning a list of pointer values that match. */
  fun resolvePath(value: Element, expression: DocPath): List<String> {
    val root = TreeNode("", null)

    val tokens = expression.tokens()
    queryGraph(tokens, root, value, 0)

    val expandedPaths = mutableListOf<List<String>>()
    val pathTokenLength = tokens.filter { it !is PathToken.Index }.size
    root.depthFirst { leaf ->
      val pathToRoot = leaf.ancestors { node -> node.id }
      if (pathToRoot.size == pathTokenLength) {
        expandedPaths.add(pathToRoot)
      }
    }
    return expandedPaths.map {
      it.reversed().joinToString("/")
    }.filter {
      it.isNotEmpty()
    }
  }

  @Suppress("LongMethod", "CyclomaticComplexMethod", "NestedBlockDepth")
  private fun queryGraph(
    pathTokens: List<PathToken>,
    parentNode: TreeNode,
    element: Element,
    index: Int
  ) {
    logger.trace { ">>> query_graph [$pathTokens, ${parentNode.id}, $index, $element]" }

    val token = pathTokens.firstOrNull()
    if (token != null) {
      logger.trace { "next token = $token" }

      when (token) {
        is PathToken.Field -> {
          if (element.nodeName == token.name) {
            logger.trace { "Field name matches element [${token.name}, ${parentNode.id}]" }
            val node = parentNode.addChild("${token.name}[${index}]")

            val remainingTokens = pathTokens.drop(1)
            if (remainingTokens.isNotEmpty()) {
              queryAttributes(remainingTokens, node, element, index)
              queryText(remainingTokens, node, element, index)

              val nextToken = remainingTokens.firstOrNull()
              if (nextToken is PathToken.Index) {
                queryGraph(remainingTokens, node, element, index)
              } else {
                val groupedChildren = groupChildren(element)
                for (children in groupedChildren.values) {
                  for ((childIndex, child) in children.withIndex()) {
                    queryGraph(remainingTokens, node, child, childIndex)
                  }
                }
              }
            }
          }
        }
        is PathToken.Index -> {
          if (token.index == index) {
            logger.trace { "Index matches element index [$index, ${token.index}, ${parentNode.id}]" }
            val remainingTokens = pathTokens.drop(1)
            if (remainingTokens.isNotEmpty()) {
              queryAttributes(remainingTokens, parentNode, element, index)
              queryText(remainingTokens, parentNode, element, index)

              val groupedChildren = groupChildren(element)
              for (children in groupedChildren.values) {
                for ((childIndex, child) in children.withIndex()) {
                  queryGraph(remainingTokens, parentNode, child, childIndex)
                }
              }
            }
          } else {
            logger.trace { "Index does not match element index, removing [$index, ${token.index}, ${parentNode.id}]" }
            parentNode.detach()
          }
        }
        PathToken.Root -> queryGraph(pathTokens.drop(1), parentNode, element, index)
        PathToken.Star, PathToken.StarIndex -> {
          logger.trace { "* -> Adding current node to parent [${parentNode.id}, ${element.nodeName}]" }
          val childNode = parentNode.addChild("${element.nodeName}[${index}]")

          val remainingTokens = pathTokens.drop(1)
          if (remainingTokens.isNotEmpty()) {
            queryAttributes(remainingTokens, childNode, element, index)
            queryText(remainingTokens, childNode, element, index)

            val groupedChildren = groupChildren(element)
            for (children in groupedChildren.values) {
              for ((childIndex, child) in children.withIndex()) {
                queryGraph(remainingTokens, childNode, child, childIndex)
              }
            }
          }
        }
      }
    }
  }

  private fun queryText(
    tokens: List<PathToken>,
    parentNode: TreeNode,
    element: Element,
    index: Int
  ) {
    logger.trace { ">>> query_text [$tokens, ${parentNode.id}, $index, $element]" }

    val nextToken = tokens.firstOrNull()
    if (nextToken != null) {
      logger.trace { "next token = $nextToken"  }
      if (nextToken is PathToken.Field && nextToken.name == "#text") {
        if (text(element).trim().isNotEmpty()) {
          logger.trace { "Field name matches element text" }
          parentNode.addChild(nextToken.name)
        }
      }
    }
  }

  private fun queryAttributes(
    tokens: List<PathToken>,
    parentNode: TreeNode,
    element: Element,
    index: Int
  ) {
    logger.trace { ">>> query_attributes [$tokens, ${parentNode.id}, $index, $element]" }

    val nextToken = tokens.firstOrNull()
    if (nextToken != null) {
      logger.trace { "next token = $nextToken"  }
      if (nextToken is PathToken.Field && nextToken.name.startsWith('@')) {
        val attributeName = nextToken.name.drop(1)
        val attribute = element.attributes.getNamedItem(attributeName)
        if (attribute != null) {
          logger.trace { "Field name matches element attribute [$attributeName]" }
          parentNode.addChild(nextToken.name)
        }
      }
    }
  }

  val PATH_RE = Regex("(\\w+)\\[(\\d+)]")

  /** Returns the matching node from the XML for the given path. */
  fun resolveMatchingNode(element: Element, path: String): XmlResult? {
    logger.trace { ">>> resolve_matching_node [$path, $element]" }

    val paths = path.split("/")
      .filter { it.isNotEmpty() }
    val firstPart = paths.firstOrNull()
    return if (firstPart != null) {
      val captures = PATH_RE.find(firstPart)
      if (captures != null) {
        val name = captures.groupValues[1]
        val index = handleWith<Int> { captures.groupValues[2].toInt() }.getOr(-1)
        if (index == 0 && name == element.nodeName) {
          if (paths.size > 1) {
            matchNext(element, paths.drop(1))
          } else {
            XmlResult.ElementNode(element)
          }
        } else {
          null
        }
      } else {
        null
      }
    } else {
      null
    }
  }

  private fun matchNext(element: Element, paths: List<String>): XmlResult? {
    logger.trace { ">>> match_next [$paths, $element]" }

    val firstPart = paths.firstOrNull()
    return if (firstPart != null) {
      if (firstPart.startsWith('@')) {
        val attribute = element.attributes.getNamedItem(firstPart.drop(1))
        if (attribute != null) {
          XmlResult.Attribute(attribute.nodeName, attribute.nodeValue)
        } else {
          null
        }
      } else if (firstPart == "#text") {
        val text = text(element)
        if (text.isEmpty()) {
          null
        } else {
          XmlResult.TextNode(text)
        }
      } else {
        val captures = PATH_RE.find(firstPart)
        if (captures != null) {
          val name = captures.groupValues[1]
          val index = handleWith<Int> { captures.groupValues[2].toInt() }.getOr(-1)
          val groupedChildren = groupChildren(element)
          val child = groupedChildren[name]?.getOrNull(index)
          if (child != null) {
            if (paths.size > 1) {
              matchNext(child, paths.drop(1))
            } else {
              XmlResult.ElementNode(child)
            }
          } else {
            null
          }
        } else {
          null
        }
      }
    } else {
      null
    }
  }

  private fun text(element: Element): String {
    return (0..<element.childNodes.length)
      .map { index -> element.childNodes.item(index) }
      .filter { it.nodeType == Node.TEXT_NODE }
      .joinToString("") { (it as Text).wholeText }
      .trim()
  }
}
