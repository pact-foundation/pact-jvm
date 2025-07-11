package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.matchers.engine.NodeValue.Companion.escape
import au.com.dius.pact.core.model.XmlUtils.renderXml

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
}
