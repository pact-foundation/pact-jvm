package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.matchers.BodyMismatchFactory
import au.com.dius.pact.core.matchers.domatch
import au.com.dius.pact.core.model.Into
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.core.support.json.JsonValue
import org.apache.commons.text.StringEscapeUtils.escapeJson
import java.util.Base64

/** Enum for the value stored in a leaf node */
sealed class NodeValue: Into<NodeValue> {
  /** Default is no value */
  data object NULL: NodeValue()

  /** A string value */
  data class STRING(val string: String): NodeValue()

  /** Boolean value */
  data class BOOL(val bool: Boolean): NodeValue()

  /** Multi-string map (String key to one or more string values) */
  data class MMAP(val entries: Map<String, List<String>>): NodeValue()

  /** List of String values */
  data class SLIST(val items: List<String>): NodeValue()

  /** Byte Array */
  data class BARRAY(val bytes: ByteArray): NodeValue() {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as BARRAY

      return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
      return bytes.contentHashCode()
    }
  }

  /** Namespaced value */
  data class NAMESPACED(val name: String, val value: String): NodeValue()

  /** Unsigned integer */
  data class UINT(val uint: UInt): NodeValue()

  /** JSON */
  data class JSON(val json: JsonValue): NodeValue()

  /** Key/Value Pair */
  data class ENTRY(val key: String, val value: NodeValue): NodeValue()

  /** List of values */
  data class LIST(val items: List<NodeValue>): NodeValue()

//  /// XML
//  #[cfg(feature = "xml")]
//  XML(XmlValue)

  /** Returns the encoded string form of the node value */
  @Suppress("LongMethod", "CyclomaticComplexMethod")
  fun strForm(): String {
    return when (this) {
      is NULL -> "NULL"
      is STRING -> escape(this.string)
      is BOOL -> "BOOL(${this.bool})"
      is MMAP -> {
        val buffer = StringBuilder()
        buffer.append('{')

        var first = true
        val keys = this.entries.keys.sorted()
        for (key in keys) {
          if (first) {
            first = false
          } else {
            buffer.append(", ")
          }
          buffer.append(escape(key))

          val values = this.entries[key]
          if (values.isNullOrEmpty()) {
            buffer.append(": []")
          } else if (values.size == 1) {
            buffer.append(": ");
            buffer.append(escape(values[0]))
          } else {
            buffer.append(": [")
            buffer.append(values.joinToString(", ") { v -> escape(v) })
            buffer.append(']')
          }
        }

        buffer.append('}')
        buffer.toString()
      }
      is SLIST -> {
        val buffer = StringBuilder()
        buffer.append('[')
        buffer.append(this.items.joinToString(", ") { v -> escape(v) })
        buffer.append(']')
        buffer.toString()
      }
      is BARRAY -> {
        val buffer = StringBuilder()
        buffer.append("BYTES(")
        buffer.append(this.bytes.size.toString())
        buffer.append(", ")
        buffer.append(Base64.getEncoder().encodeToString(this.bytes))
        buffer.append(')')
        buffer.toString()
      }
      is NAMESPACED -> {
        val buffer = StringBuilder()
        buffer.append(this.name)
        buffer.append(':')
        buffer.append(this.value)
        buffer.toString()
      }
      is UINT -> "UINT(${this.uint})"
      is JSON -> "json:${this.json.serialise()}"
      is ENTRY -> {
        val buffer = StringBuilder()
        buffer.append(escape(this.key))
        buffer.append(" -> ")
        buffer.append(this.value.strForm())
        buffer.toString()
      }
      is LIST -> {
        val buffer = StringBuilder()
        buffer.append('[')
        buffer.append(this.items.joinToString(", ") { v -> v.strForm() })
        buffer.append(']')
        buffer.toString()
      }
//      #[cfg(feature = "xml")]
//      NodeValue::XML(node) -> match node {
//        XmlValue::Element(element) -> format!("xml:{}", escape(element.to_string().as_str())),
//        XmlValue::Text(text) -> format!("xml:text:{}", escape(text.as_str())),
//        XmlValue::Attribute(name, value) -> format!("xml:attribute:{}={}",
//          escape(name.as_str()), escape(value.as_str()))
//      }
    }
  }

  /** Returns the type of the value */
  fun valueType(): String {
    return when (this) {
      is BARRAY -> "Byte Array"
      is BOOL -> "Boolean"
      is ENTRY -> "Entry"
      is JSON -> "JSON"
      is LIST -> "List"
      is MMAP -> "Multi-Value String Map"
      is NAMESPACED -> "Namespaced Value"
      NULL -> "NULL"
      is SLIST -> "String List"
      is STRING -> "String"
      is UINT -> "Unsigned Integer"
//      #[cfg(feature = "xml")]
//      NodeValue::XML(_) => "XML"
    }
  }

//
//  /// If this value is an XML value, returns it, otherwise returns None
//  #[cfg(feature = "xml")]
//  pub fn as_xml(&self) -> Option<XmlValue> {
//    match self {
//      NodeValue::XML(xml) => Some(xml.clone()),
//      _ => None
//    }
//  }

//  /// If this value is a bool value, returns it, otherwise returns None
//  pub fn as_bool(&self) -> Option<bool> {
//    match self {
//      NodeValue::BOOL(b) => Some(*b),
//      _ => None
//    }
//  }
//
//  /// If this value is an UInt value, returns it, otherwise returns None
//  pub fn as_uint(&self) -> Option<u64> {
//    match self {
//      NodeValue::UINT(u) => Some(*u),
//      _ => None
//    }
//  }
//
//  /// If this value is a string value, returns it, otherwise returns None
//  pub fn as_slist(&self) -> Option<Vec<String>> {
//    match self {
//      NodeValue::SLIST(list) => Some(list.clone()),
//      _ => None
//    }
//  }

  /** Calculates an AND of two values */
  fun and(other: NodeValue): NodeValue {
    return when (this) {
      is BOOL -> BOOL(this.bool && other.truthy())
      NULL -> other
      else -> BOOL(truthy() && other.truthy())
    }
  }

  /** Calculates an OR of two values */
  fun or(other: NodeValue): NodeValue {
    return when (this) {
      is BOOL -> BOOL(this.bool || other.truthy())
      NULL -> other
      else -> BOOL(truthy() || other.truthy())
    }
  }

  /** Convert this value into a boolean using a "truthy" test */
  fun truthy(): Boolean {
    return when (this) {
      is BARRAY -> this.bytes.isNotEmpty()
      is BOOL -> this.bool
      is LIST -> this.items.isNotEmpty()
      is MMAP -> this.entries.isNotEmpty()
      is SLIST -> this.items.isNotEmpty()
      is STRING -> this.string.isNotEmpty()
      is UINT -> this.uint != 0.toUInt()
      else -> false
    }
  }

//  /// Converts this value into a list of values
//  pub fn to_list(&self) -> Vec<NodeValue> {
//    match self {
//      NodeValue::MMAP(entries) => {
//        entries.iter()
//          .map(|(k, v)| NodeValue::ENTRY(k.clone(), Box::new(NodeValue::SLIST(v.clone()))))
//          .collect()
//      }
//      NodeValue::SLIST(list) => {
//        list.iter()
//          .map(|v| NodeValue::STRING(v.clone()))
//          .collect()
//      }
//      NodeValue::JSON(json) => match json {
//        Value::Array(a) => {
//          a.iter()
//            .map(|v| NodeValue::JSON(v.clone()))
//            .collect()
//        }
//        _ => vec![ self.clone() ]
//      }
//      NodeValue::LIST(list) => list.clone(),
//      _ => vec![ self.clone() ]
//    }
//  }
//}
//
//impl From<String> for NodeValue {
//  fn from(value: String) -> Self {
//    NodeValue::STRING(value.clone())
//  }
//}
//
//impl From<&str> for NodeValue {
//  fn from(value: &str) -> Self {
//    NodeValue::STRING(value.to_string())
//  }
//}
//
//impl From<usize> for NodeValue {
//  fn from(value: usize) -> Self {
//    NodeValue::UINT(value as u64)
//  }
//}
//
//impl From<u64> for NodeValue {
//  fn from(value: u64) -> Self {
//    NodeValue::UINT(value)
//  }
//}
//
//impl From<Value> for NodeValue {
//  fn from(value: Value) -> Self {
//    NodeValue::JSON(value.clone())
//  }
//}
//
//impl From<&Value> for NodeValue {
//  fn from(value: &Value) -> Self {
//    NodeValue::JSON(value.clone())
//  }
//}
//
//impl From<HashMap<&str, Value>> for NodeValue {
//  fn from(value: HashMap<&str, Value>) -> Self {
//    let json = Object(value.iter().map(|(k, v)| (k.to_string(), v.clone())).collect());
//    NodeValue::JSON(json)
//  }
//}
//
//impl From<Vec<String>> for NodeValue {
//  fn from(value: Vec<String>) -> Self {
//    NodeValue::SLIST(value)
//  }
//}
//
//impl From<Vec<&String>> for NodeValue {
//  fn from(value: Vec<&String>) -> Self {
//    NodeValue::SLIST(value.iter().map(|v| (*v).clone()).collect())
//  }
//}
//
//#[cfg(feature = "xml")]
//impl From<Element> for NodeValue {
//  fn from(value: Element) -> Self {
//    NodeValue::XML(XmlValue::Element(value.clone()))
//  }
//}
//
//#[cfg(feature = "xml")]
//impl From<&Element> for NodeValue {
//  fn from(value: &Element) -> Self {
//    NodeValue::XML(XmlValue::Element(value.clone()))
//  }
//}
  /** If this value is a JSON value, returns it, otherwise returns null */
  fun asJson(): JsonValue? {
    return if (this is JSON) {
      json
    } else {
      null
    }
  }

  /** If this value is a String value, returns it, otherwise returns null */
  fun asString(): String? {
    return if (this is STRING) {
      string
    } else {
      null
    }
  }

  override fun into() = this

  companion object {
    @JvmStatic
    fun escape(value: String): String {
      return if (value.isEmpty()) {
        "''"
      } else if (value.any { it == '\'' || it.isWhitespace() }) {
        "'${escapeJson(value).replace("'", "\\'")}'"
      } else {
        "'$value'"
      }
    }

    fun doMatch(
      expected: NodeValue,
      actual: NodeValue,
      matcher: MatchingRule,
      cascaded: Boolean,
      actionPath: List<String>
    ): String? {
    //    match self {
    //      NodeValue::NULL => Value::Null.matches_with(actual.as_json().unwrap_or_default(), matcher, cascaded),
    //      NodeValue::STRING(s) => if let Some(actual_str) = actual.as_string() {
    //        s.matches_with(actual_str, matcher, cascaded)
    //      } else if let Some(list) = actual.as_slist() {
    //        let result = list.iter()
    //          .map(|item| s.matches_with(item, matcher, cascaded))
    //          .filter_map(|r| match r {
    //            Ok(_) => None,
    //            Err(err) => Some(err.to_string())
    //          })
    //          .collect_vec();
    //        if result.is_empty() {
    //          Ok(())
    //        } else {
    //          Err(anyhow!(result.join(", ")))
    //        }
    //      } else {
    //        s.matches_with(actual.to_string(), matcher, cascaded)
    //      },
    //      NodeValue::BOOL(b) => b.matches_with(actual.as_bool().unwrap_or_default(), matcher, cascaded),
    //      NodeValue::UINT(u) => u.matches_with(actual.as_uint().unwrap_or_default(), matcher, cascaded),
    //      NodeValue::JSON(json) => json.matches_with(actual.as_json().unwrap_or_default(), matcher, cascaded),
    //      NodeValue::SLIST(list) => if let Some(actual_list) = actual.as_slist() {
    //        list.matches_with(&actual_list, matcher, cascaded)
    //      } else {
    //        let actual_str = if let Some(actual_str) = actual.as_string() {
    //          actual_str
    //        } else {
    //          actual.to_string()
    //        };
    //        list.matches_with(&vec![actual_str], matcher, cascaded)
    //      }
    //      #[cfg(feature = "xml")]
    //      NodeValue::XML(xml_value) => if let Some(actual) = actual.as_xml() {
    //        xml_value.matches_with(actual, matcher, cascaded)
    //      } else {
    //        Err(anyhow!("Was expecting an XML value but got {}", actual))
    //      },
    //      _ => Err(anyhow!("Matching rules can not be applied to {} values", self.str_form()))
    //    }
    //  }
      return when (expected) {
        is BARRAY -> TODO()
        is BOOL -> TODO()
        is ENTRY -> TODO()
        is JSON -> TODO()
        is LIST -> TODO()
        is MMAP -> TODO()
        is NAMESPACED -> TODO()
        NULL -> TODO()
        is SLIST -> TODO()
        is STRING -> {
          if (actual is STRING) {
            val result = domatch(matcher, actionPath, expected.string, actual.string, BodyMismatchFactory, cascaded)
            if (result.isNotEmpty()) {
              result.joinToString(", ") { it.mismatch }
            } else {
              null
            }
          } else if (actual is SLIST) {
            TODO()
          } else {
            TODO()
          }
        }
        is UINT -> TODO()
      }
    }
  }
}
