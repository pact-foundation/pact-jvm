package au.com.dius.pact.core.matchers

import org.w3c.dom.Node

/**
 * Namespace-aware XML node qualified names.
 *
 * Used for comparison and display purposes. Uses the XML namespace and local name if present in the [Node].
 * Falls back to using the default node name if a namespace isn't present.
 */
class QualifiedName(node: Node) {
  val namespaceUri: String? = node.namespaceURI
  val localName: String? = node.localName
  val nodeName: String = node.nodeName

  /**
   * When both do not have a namespace, check equality using node name.
   * Otherwise, check equality using both namespace and local name.
   */
  override fun equals(other: Any?): Boolean = when (other) {
    is QualifiedName -> {
      when {
        this.namespaceUri == null && other.namespaceUri == null -> other.nodeName == nodeName
        else -> other.namespaceUri == namespaceUri && other.localName == localName
      }
    }
    else -> false
  }

  /**
   * When a namespace isn't present, return the hash of the node name.
   * Otherwise, return the hash of the namespace and local name.
   */
  override fun hashCode(): Int = when (namespaceUri) {
    null -> nodeName.hashCode()
    else -> 31 * (31 + namespaceUri.hashCode()) + localName.hashCode()
  }

  /**
   * Returns the qualified name using Clark-notation if
   * a namespace is present, otherwise returns the node name.
   *
   * Clark-notation uses the format `{namespace}localname`
   * per https://sabre.io/xml/clark-notation/.
   *
   * @see Node.getNodeName
   */
  override fun toString(): String {
    return when (namespaceUri) {
      null -> nodeName
      else -> "{$namespaceUri}$localName"
    }
  }
}
