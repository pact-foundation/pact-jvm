package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.matchers.engine.NodeValue.Companion.escape
import au.com.dius.pact.core.model.XmlResult
import au.com.dius.pact.core.model.XmlUtils.renderXml
import au.com.dius.pact.core.support.Result
import org.w3c.dom.Node

/**
 * Enum to store different XML nodes
 */
sealed class XmlValue {
  /** XML element */
  data class Element(val element: org.w3c.dom.Element): XmlValue() {
    override fun toString() = renderXml(element)
  }

  /** XML text */
  data class Text(val text: String): XmlValue() {
    override fun toString() = escape(text)
  }

  /** Attribute */
  data class Attribute(val name: String, val value: String): XmlValue() {
    override fun toString() = "@$name=${escape(value)}"
  }

  /** Returns the value if it is an XML element */
  fun asElement(): org.w3c.dom.Element? = (this as? Element)?.element

  /** Returns the value if it is XML text */
  fun asText(): String? = (this as? Text)?.text

  /** Returns the value if it is an XML attribute */
  fun asAttribute(): Pair<String, String>? {
    return if (this is Attribute) {
      this.name to this.value
    } else {
      null
    }
  }

  companion object {
    fun fromNode(node: Node): Result<XmlValue, String> {
      return when (node.nodeType) {
        Node.ELEMENT_NODE -> Result.Ok(Element(node as org.w3c.dom.Element))
        Node.TEXT_NODE -> Result.Ok(Text(node.textContent))
        Node.ATTRIBUTE_NODE -> Result.Ok(Attribute(node.nodeName, node.nodeValue))
        else -> Result.Err("$node is not a supported XML node type")
      }
    }

    fun fromResult(result: XmlResult): XmlValue {
      return when (result) {
        is XmlResult.Attribute -> Attribute(result.name, result.value)
        is XmlResult.ElementNode -> Element(result.element)
        is XmlResult.TextNode -> Text(result.text)
      }
    }
  }
}
