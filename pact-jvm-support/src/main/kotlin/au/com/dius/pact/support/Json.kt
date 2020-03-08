package au.com.dius.pact.support

import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonNull
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.toJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser

/**
 * JSON support functions
 */
object Json {

  val numberAdapter = NumberSerializer()
  val gsonPretty: Gson = GsonBuilder().setPrettyPrinting()
    .serializeNulls()
    .disableHtmlEscaping()
    .registerTypeHierarchyAdapter(Number::class.java, numberAdapter).create()
  val gson: Gson = GsonBuilder().serializeNulls().disableHtmlEscaping()
    .registerTypeHierarchyAdapter(Number::class.java, numberAdapter).create()

  /**
   * Converts an Object graph to a JSON Object
   */
  fun toJson(any: Any?): JsonElement {
    return when (any) {
      is JsonElement -> any
      is Number -> any.toJson()
      is String -> any.toJson()
      is Boolean -> any.toJson()
      is Char -> any.toJson()
      is List<*> -> jsonArray(any.map { toJson(it) })
      is Map<*, *> -> jsonObject(any.entries.map { it.key.toString() to toJson(it.value) })
      else -> jsonNull
    }
  }

  /**
   * Converts a JSON object to a raw string if it is a string value, else just calls toString()
   */
  fun toString(jsonElement: JsonElement?) = when {
    jsonElement == null -> jsonNull.toString()
    jsonElement.isJsonPrimitive -> {
      val value = jsonElement.asJsonPrimitive
      when {
        value.isString -> value.asString
        else -> value.toString()
      }
    }
    else -> jsonElement.toString()
  }

  /**
   * Converts a JSON object to the Map of values
   */
  fun toMap(jsonElement: JsonElement?) = when {
    jsonElement != null && jsonElement.isJsonObject -> toMapValue(jsonElement) as Map<String, Any?>
    else -> emptyMap()
  }

  /**
   * Converts a JSON object to the List of values
   */
  fun toList(jsonElement: JsonElement?) = when {
    jsonElement != null && jsonElement.isJsonArray -> toMapValue(jsonElement) as List<Any?>
    else -> emptyList()
  }

  private fun toMapValue(json: JsonElement): Any? = when {
    json.isJsonObject -> json.obj.entrySet().associate {
      it.key to toMapValue(it.value)
    }
    json.isJsonArray -> json.array.map { toMapValue(it) }
    json.isJsonPrimitive -> {
      val primitive = json.asJsonPrimitive
      when {
        primitive.isString -> primitive.asString
        primitive.isBoolean -> primitive.asBoolean
        primitive.isNumber -> primitive.asNumber
        else -> primitive.toString()
      }
    }
    json.isJsonNull -> null
    else -> json.toString()
  }

  fun extractFromJson(json: JsonElement, vararg s: String): Any? {
    return if (json.isJsonObject && s.size == 1) {
      json.obj[s.first()]
    } else if (json.isJsonObject && json.obj.has(s.first())) {
      val map = json.obj[s.first()]
      if (map.isJsonObject) {
        extractFromJson(map.obj, *s.drop(1).toTypedArray())
      } else {
        null
      }
    } else {
      null
    }
  }

  fun fromJson(json: JsonElement?): Any? = when {
    json == null || json.isJsonNull -> null
    json.isJsonObject -> json.obj.entrySet().associate { it.key to fromJson(it.value) }
    json.isJsonArray -> json.array.map { fromJson(it) }
    else -> {
      val primitive = json.asJsonPrimitive
      when {
        primitive.isBoolean -> primitive.asBoolean
        primitive.isNumber -> primitive.asNumber
        primitive.isString -> primitive.asString
        else -> primitive.toString()
      }
    }
  }

  fun prettyPrint(json: String) = gsonPretty.toJson(JsonParser().parse(json))

  fun exceptionToJson(exp: Exception) = jsonObject("message" to exp.message,
    "exceptionClass" to exp.javaClass.name)
}
