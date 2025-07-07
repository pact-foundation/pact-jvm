package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.matchers.BodyItemMatchResult
import au.com.dius.pact.core.matchers.BodyMismatchFactory
import au.com.dius.pact.core.matchers.JsonContentMatcher
import au.com.dius.pact.core.matchers.Matchers.compareLists
import au.com.dius.pact.core.matchers.MatchingContext
import au.com.dius.pact.core.matchers.domatch
import au.com.dius.pact.core.model.Into
import au.com.dius.pact.core.model.constructPath
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
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

//  /// If this value is an XML value, returns it, otherwise returns None
//  #[cfg(feature = "xml")]
//  pub fn as_xml(&self) -> Option<XmlValue> {
//    match self {
//      NodeValue::XML(xml) => Some(xml.clone()),
//      _ => None
//    }
//  }

  /** If this value is a bool value, returns it, otherwise returns null */
  fun asBool(): Boolean? {
    return if (this is BOOL) {
      bool
    } else {
      null
    }
  }

  /** If this value is an UInt value, returns it, otherwise returns null */
  fun asUInt(): UInt? {
    return if (this is UINT) {
      uint
    } else {
      null
    }
  }

  /** If this value is a string list, returns it, otherwise returns null */
  fun asSList(): List<String>? {
    return if (this is SLIST) {
      items
    } else {
      null
    }
  }

  override fun into() = this

  /**
   * Returns the inner value if the NodeValue
   */
  fun unwrap(): Any? {
    return when (this) {
      is BARRAY -> this.bytes
      is BOOL -> this.bool
      is ENTRY -> Pair(this.key, this.value)
      is JSON -> this.json
      is LIST -> this.items.map { it.unwrap() }
      is MMAP -> this.entries
      is NAMESPACED -> Pair(this.name, this.value)
      NULL -> null
      is SLIST -> this.items
      is STRING -> this.string
      is UINT -> this.uint
    }
  }

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

    @Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
    fun doMatch(
      expected: NodeValue,
      actual: NodeValue,
      matcher: MatchingRule,
      cascaded: Boolean,
      actionPath: List<String>,
      context: PlanMatchingContext
    ): String? {
    //      #[cfg(feature = "xml")]
    //      NodeValue::XML(xml_value) => if let Some(actual) = actual.as_xml() {
    //        xml_value.matches_with(actual, matcher, cascaded)
    //      } else {
    //        Err(anyhow!("Was expecting an XML value but got {}", actual))
    //      },
      return when (expected) {
        is JSON -> {
          if (actual is NULL || actual is JSON) {
            // TODO: need a way to pass allowUnexpectedKeys here
            val actualJson = if (actual is JSON) {
              actual.json
            } else {
              JsonValue.Null
            }
            val result = JsonContentMatcher.compare(listOf("$"), expected.json, actualJson, context.matchingContext)
            if (result.any { it.result.isNotEmpty() }) {
              result.flatMap { mismatches -> mismatches.result }
                .joinToString(", ") { it.mismatch }
            } else {
              null
            }
          } else {
            "Expected a value of type '${expected.valueType()}' but got '${actual.valueType()}'"
          }
        }
        is LIST -> {
          val items = when (actual) {
            is LIST -> actual.items.map { it.unwrap() }
            is SLIST -> actual.items
            else -> listOf(actual.unwrap())
          }
          val result = compareLists(actionPath, matcher, expected.items, items, context.matchingContext, { "" }, cascaded) {
              p, expected, actual, context ->
            val result = domatch(matcher, p, expected, actual, BodyMismatchFactory, cascaded, context)
            listOf(BodyItemMatchResult(constructPath(p), result))
          }.flatMap { it.result }
          if (result.isNotEmpty()) {
            result.joinToString(", ") { it.mismatch }
          } else {
            null
          }
        }
        is MMAP -> TODO()
        is SLIST -> {
          val items = when (actual) {
            is LIST -> actual.items.map { it.unwrap() }
            is SLIST -> actual.items
            else -> listOf(actual.unwrap())
          }
          val result = compareLists(actionPath, matcher, expected.items, items, context.matchingContext, { "" }, cascaded) {
              p, expected, actual, context ->
            val result = domatch(matcher, p, expected, actual, BodyMismatchFactory, cascaded, context)
            listOf(BodyItemMatchResult(constructPath(p), result))
          }.flatMap { it.result }
          if (result.isNotEmpty()) {
            result.joinToString(", ") { it.mismatch }
          } else {
            null
          }
        }
        is STRING -> {
          if (actual is STRING) {
            val result = domatch(matcher, actionPath, expected.string, actual.string, BodyMismatchFactory, cascaded, context.matchingContext)
            if (result.isNotEmpty()) {
              result.joinToString(", ") { it.mismatch }
            } else {
              null
            }
          } else if (actual is SLIST) {
            val result = compareLists(actionPath, matcher, listOf(expected.string), actual.items, context.matchingContext, { "" }, cascaded) {
                p, expected, actual, context ->
                  val result = domatch(matcher, p, expected, actual, BodyMismatchFactory, cascaded, context)
                  listOf(BodyItemMatchResult(constructPath(p), result))
            }.flatMap { it.result }
            if (result.isNotEmpty()) {
              result.joinToString(", ") { it.mismatch }
            } else {
              null
            }
          } else {
            "Expected a value of type '${expected.valueType()}' but got '${actual.valueType()}'"
          }
        }
        else -> {
          val result = domatch(matcher, actionPath, expected.unwrap(), actual.unwrap(), BodyMismatchFactory, cascaded, context.matchingContext)
          if (result.isNotEmpty()) {
            result.joinToString(", ") { it.mismatch }
          } else {
            null
          }
        }
      }
    }
  }
}

fun NodeValue?.orDefault() = this ?: NodeValue.NULL
