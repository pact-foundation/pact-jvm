package au.com.dius.pact.core.model

import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


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
  fun groupChildren(element: Node): Map<String, MutableList<Node>> {
    val childNodes = element.childNodes
    return (0..<childNodes.length)
      .map { index -> childNodes.item(index) }
      .filter { it.nodeType == Node.ELEMENT_NODE }
      .fold(mutableMapOf()) { acc, node ->
        val tagName = node.nodeName
        if (acc.contains(tagName)) {
          acc[tagName]!!.add(node)
        } else {
          acc[tagName] = mutableListOf(node)
        }
        acc
      }
  }
}
