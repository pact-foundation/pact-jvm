package au.com.dius.pact.consumer.xml

import au.com.dius.pact.consumer.dsl.Matcher
import au.com.dius.pact.core.model.generators.Category.BODY
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.Category
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import org.w3c.dom.DOMImplementation
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.util.function.Consumer
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private fun matcherKey(path: List<String>, vararg key: String): String {
  return (path + key).reduce { acc, s ->
    if (s.startsWith('[')) {
      acc + s
    } else {
      "$acc.$s"
    }
  }
}

class PactXmlBuilder @JvmOverloads constructor (
  var rootName: String,
  var rootNameSpace: String? = null,
  var namespaces: Map<String, String> = emptyMap(),
  var version: String? = null,
  var charset: String? = null,
  var standalone: Boolean = false
) {
  val generators: Generators = Generators()
  val matchingRules: Category = Category("body")

  lateinit var doc: Document
  private lateinit var dom: DOMImplementation

  fun build(cl: Consumer<XmlNode>): PactXmlBuilder {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    this.dom = builder.domImplementation
    this.doc = if (rootNameSpace != null) {
      dom.createDocument(rootNameSpace, "ns:$rootName", null)
    } else {
      builder.newDocument()
    }
    if (version != null) {
      doc.xmlVersion = version
    }
    doc.xmlStandalone = standalone
    val root = if (doc.documentElement == null) {
      val element = doc.createElement(rootName)
      doc.appendChild(element)
      element
    } else doc.documentElement
    val xmlNode = XmlNode(this, root, listOf("$", qualifiedName(rootName)))
    cl.accept(xmlNode)
    return this
  }

  fun qualifiedName(name: String): String {
    return if (rootNameSpace.isNullOrEmpty()) {
      name
    } else {
      "ns:$name"
    }
  }

  @JvmOverloads
  fun asBytes(charset: Charset? = null): ByteArray {
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    if (standalone) {
      transformer.setOutputProperty(OutputKeys.STANDALONE, "yes")
    }

    val source = DOMSource(doc)
    val outputStream = ByteArrayOutputStream()
    val result = if (charset != null) {
      StreamResult(OutputStreamWriter(outputStream, charset))
    } else {
      StreamResult(outputStream)
    }
    transformer.transform(source, result)
    return outputStream.toByteArray()
  }

  override fun toString() = String(asBytes())

  /**
   * Sets the name of the root name
   */
  fun withRootName(name: String): PactXmlBuilder {
    this.rootName = name
    return this
  }

  /**
   * Sets the namespace of the root node
   */
  fun withRootNameSpace(nameSpace: String): PactXmlBuilder {
    this.rootNameSpace = nameSpace
    return this
  }

  /**
   * Namespaces to define on the root name
   */
  fun withNamespaces(namespaces: Map<String, String>): PactXmlBuilder {
    this.namespaces = namespaces
    return this
  }

  /**
   * Sets the version on the XML descriptor. Defaults to '1.0'.
   */
  fun withVersion(version: String): PactXmlBuilder {
    this.version = version
    return this
  }

  /**
   * Sets the charset on the XML descriptor. Defaults to 'UTF-8'
   */
  fun withCharset(charset: String): PactXmlBuilder {
    this.charset = charset
    return this
  }

  /**
   * Sets the standalone flag on the XML descriptor. Default is set ('yes')
   */
  fun withStandalone(standalone: Boolean): PactXmlBuilder {
    this.standalone = standalone
    return this
  }
}

class XmlNode(private val builder: PactXmlBuilder, private val element: Element, private val path: List<String>) {
  fun setAttributes(attributes: Map<String, Any?>) {
    setElementAttributes(attributes, element)
  }

  @JvmOverloads
  fun eachLike(name: String, examples: Int = 1, attributes: Map<String, Any?> = emptyMap(), cl: Consumer<XmlNode>? = null) {
    builder.matchingRules.addRule(matcherKey(path, name), TypeMatcher)
    val element = builder.doc.createElement(name)
    val node = XmlNode(builder, element, this.path + element.tagName)
    node.setAttributes(attributes)
    cl?.accept(node)
    this.element.appendChild(element)
    (2..examples).forEach { _ ->
      this.element.appendChild(element.cloneNode(true))
    }
  }

  @JvmOverloads
  fun appendElement(name: String, attributes: Map<String, Any?> = emptyMap(), cl: Consumer<XmlNode>? = null) {
    val element = builder.doc.createElement(name)
    val node = XmlNode(builder, element, this.path + element.tagName)
    node.setAttributes(attributes)
    cl?.accept(node)
    this.element.appendChild(element)
  }

  @JvmOverloads
  fun appendElement(name: String, attributes: Map<String, Any?> = emptyMap(), contents: String) {
    val element = builder.doc.createElement(name)
    val node = XmlNode(builder, element, this.path + element.tagName)
    node.setAttributes(attributes)
    element.textContent = contents
    this.element.appendChild(element)
  }

  @JvmOverloads
  fun appendElement(name: String, attributes: Map<String, Any?> = emptyMap(), contents: Matcher) {
    val element = builder.doc.createElement(name)
    setElementAttributes(attributes, element)
    element.textContent = contents.value.toString()
    builder.matchingRules.addRule(matcherKey(path, name, "#text"), contents.matcher!!)
    if (contents.generator != null) {
      builder.generators.addGenerator(BODY, matcherKey(path, name, "#text"), contents.generator!!)
    }
    this.element.appendChild(element)
  }

  private fun setElementAttributes(attributes: Map<String, Any?>, element: Element) {
    attributes.forEach {
      if (it.value is Matcher) {
        val matcherDef = it.value as Matcher
        builder.matchingRules.addRule(matcherKey(path, "['@${it.key}']"), matcherDef.matcher!!)
        if (matcherDef.generator != null) {
          builder.generators.addGenerator(BODY, matcherKey(path, "['@${it.key}']"), matcherDef.generator!!)
        }
        element.setAttribute(it.key, matcherDef.value.toString())
      } else {
        element.setAttribute(it.key, it.value.toString())
      }
    }
  }
}
