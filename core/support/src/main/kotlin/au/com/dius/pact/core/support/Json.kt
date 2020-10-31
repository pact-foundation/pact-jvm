package au.com.dius.pact.core.support

import au.com.dius.pact.core.support.Json.toJson
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonToken
import au.com.dius.pact.core.support.json.JsonValue
import org.apache.commons.lang3.text.translate.AggregateTranslator
import org.apache.commons.lang3.text.translate.EntityArrays
import org.apache.commons.lang3.text.translate.JavaUnicodeEscaper
import org.apache.commons.lang3.text.translate.LookupTranslator
import java.io.Writer
import java.math.BigInteger

/**
 * JSON support functions
 */
object Json {
  /**
   * Converts an Object graph to a JSON Object
   */
  @JvmStatic
  fun toJson(any: Any?): JsonValue {
    return when (any) {
      is JsonValue -> any
      is Number -> any.toJsonValue()
      is String -> any.toJsonValue()
      is Boolean -> any.toJsonValue()
      is Char -> any.toJsonValue()
      is List<*> -> JsonValue.Array(any.map { toJson(it) }.toMutableList())
      is Array<*> -> JsonValue.Array(any.map { toJson(it) }.toMutableList())
      is Map<*, *> -> JsonValue.Object(any.entries.associate { it.key.toString() to toJson(it.value) }.toMutableMap())
      else -> JsonValue.Null
    }
  }

  /**
   * Converts a JSON object to a raw string if it is a string value, else just calls toString()
   */
  fun toString(json: JsonValue?): String = json?.asString() ?: json?.serialise() ?: "null"

  /**
   * Converts a JSON object to the Map of values
   */
  fun toMap(json: JsonValue?) = when (json) {
    is JsonValue.Object -> fromJson(json) as Map<String, Any?>
    else -> emptyMap()
  }

  /**
   * Converts a JSON object to the List of values
   */
  fun toList(json: JsonValue?) = when (json) {
    is JsonValue.Array -> fromJson(json) as List<Any?>
    else -> emptyList()
  }

  fun extractFromJson(json: JsonValue, vararg s: String): Any? {
    return if (json is JsonValue.Object && s.size == 1) {
      json[s.first()]
    } else if (json is JsonValue.Object && json.has(s.first())) {
      val map = json[s.first()]
      if (map is JsonValue.Object) {
        extractFromJson(map, *s.drop(1).toTypedArray())
      } else {
        null
      }
    } else {
      null
    }
  }

  fun fromJson(json: JsonValue?): Any? = when {
    json == null || json is JsonValue.Null -> null
    json is JsonValue.Object -> json.entries.entries.associate { it.key to fromJson(it.value) }
    json is JsonValue.Array -> json.values.map { fromJson(it) }
    json.isBoolean -> json.asBoolean()
    json.isNumber -> json.asNumber()
    json is JsonValue.StringValue -> json.asString()
    else -> json.toString()
  }

  fun prettyPrint(json: String) = JsonParser.parseString(json).prettyPrint()

  fun prettyPrint(json: String, writer: Writer) {
    writer.write(JsonParser.parseString(json).prettyPrint())
  }

  fun prettyPrint(obj: Any) = toJson(obj).prettyPrint()

  fun exceptionToJson(exp: Exception) = JsonValue.Object(mutableMapOf("message" to toJson(exp.message),
    "exceptionClass" to toJson(exp.javaClass.name)))

  fun toBoolean(jsonElement: JsonValue?) = when {
    jsonElement == null -> false
    jsonElement.isBoolean -> jsonElement.asBoolean()
    else -> false
  }

  fun escape(s: String): String = ESCAPE_JSON.translate(s)

  private val ESCAPE_JSON = AggregateTranslator(
    LookupTranslator(
      *arrayOf(arrayOf("\"", "\\\""), arrayOf("\\", "\\\\"))),
    LookupTranslator(*EntityArrays.JAVA_CTRL_CHARS_ESCAPE()),
    JavaUnicodeEscaper.outsideOf(32, 0x7f)
  )
}

private fun Char.toJsonValue() = JsonValue.StringValue(JsonToken.StringValue(charArrayOf(this)))

private fun Boolean.toJsonValue() = if (this) JsonValue.True
  else JsonValue.False

private fun String.toJsonValue() = JsonValue.StringValue(JsonToken.StringValue(this.toCharArray()))

private fun Number.toJsonValue(): JsonValue = when (this) {
  is Int -> JsonValue.Integer(JsonToken.Integer(this.toString().toCharArray()))
  is Long -> JsonValue.Integer(JsonToken.Integer(this.toString().toCharArray()))
  is BigInteger -> JsonValue.Integer(JsonToken.Integer(this.toString().toCharArray()))
  else -> JsonValue.Decimal(JsonToken.Decimal(this.toString().toCharArray()))
}

fun jsonArray(list: List<Any?>) = toJson(list)

fun jsonArray(value: Any?) = JsonValue.Array(mutableListOf(toJson(value)))

fun jsonObject(vararg pairs: Pair<String, Any?>) = JsonValue.Object(
  pairs.associate { it.first to toJson(it.second) }.toMutableMap())

fun jsonObject(pairs: List<Pair<String, Any?>>) = JsonValue.Object(
  pairs.associate { it.first to toJson(it.second) }.toMutableMap())
