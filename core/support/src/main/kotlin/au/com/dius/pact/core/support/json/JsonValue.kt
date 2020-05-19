package au.com.dius.pact.core.support.json

import au.com.dius.pact.core.support.Json
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.apache.commons.lang3.StringEscapeUtils
import java.math.BigDecimal
import java.math.BigInteger

sealed class JsonValue {
  data class Integer(val value: BigInteger) : JsonValue()
  data class Decimal(val value: BigDecimal) : JsonValue()
  data class StringValue(val value: String) : JsonValue()
  object True : JsonValue()
  object False : JsonValue()
  object Null : JsonValue()

  data class Array(val values: MutableList<JsonValue> = mutableListOf()) : JsonValue() {
    fun find(function: (JsonValue) -> Boolean) = values.find(function)
    operator fun set(i: Int, value: JsonValue) {
      values[i] = value
    }
    val size: Int
      get() = values.size

    fun addAll(jsonValue: JsonValue) {
      when (jsonValue) {
        is Array -> values.addAll(jsonValue.values)
        else -> values.add(jsonValue)
      }
    }

    fun last() = values.last()
  }

  data class Object(val entries: MutableMap<String, JsonValue>) : JsonValue() {
    constructor(vararg values: Pair<String, JsonValue>) : this(values.associate { it }.toMutableMap())
    operator fun get(name: String) = entries[name] ?: Null
    override fun has(field: String) = entries.containsKey(field)
    operator fun set(key: String, value: Any?) {
      entries[key] = Json.toJson(value)
    }

    fun isEmpty() = entries.isEmpty()
    fun isNotEmpty() = entries.isNotEmpty()

    val size: Int
      get() = entries.size

    fun add(key: String, value: JsonValue) {
      entries[key] = value
    }
  }

  fun asObject(): Object {
    if (this is Object) {
      return this
    } else {
      throw UnsupportedOperationException("Expected an Object, but found a $this")
    }
  }

  fun asArray(): Array {
    if (this is Array) {
      return this
    } else {
      throw UnsupportedOperationException("Expected an Array, but found a $this")
    }
  }

  fun asString(): String {
    return if (this is StringValue) {
      value
    } else {
      serialise()
    }
  }

  fun asBoolean() = when (this) {
    is True -> true
    is False -> false
    else -> throw UnsupportedOperationException("Expected a Boolean, but found a $this")
  }

  fun asNumber(): Number = when (this) {
    is Integer -> this.value
    is Decimal -> this.value
    else -> throw UnsupportedOperationException("Expected a Number, but found a $this")
  }

  operator fun get(field: Any): JsonValue = when {
    this is Object -> this.asObject()[field.toString()]
    this is Array && field is Int -> this.values[field]
    else -> throw UnsupportedOperationException("Indexed lookups only work on Arrays and Objects, not $this")
  }

  open fun has(field: String) = when (this) {
    is Object -> this.entries.containsKey(field)
    else -> false
  }

  fun serialise(): String {
    return when (this) {
      is Null -> "null"
      is Decimal -> this.value.toString()
      is Integer -> this.value.toString()
      is StringValue -> "\"${StringEscapeUtils.escapeJson(this.value)}\""
      is True -> "true"
      is False -> "false"
      is Array -> "[${this.values.joinToString(",") { it.serialise() }}]"
      is Object -> "{${this.entries.entries.joinToString(",") { "\"${it.key}\":" + it.value.serialise() }}}"
    }
  }

  fun toGson(): JsonElement {
    return when (this) {
      is Null -> JsonNull.INSTANCE
      is Decimal -> JsonPrimitive(this.value)
      is Integer -> JsonPrimitive(this.value)
      is StringValue -> JsonPrimitive(this.value)
      is True -> JsonPrimitive(true)
      is False -> JsonPrimitive(false)
      is Array -> {
        val array = JsonArray()
        this.values.forEach { array.add(it.toGson()) }
        array
      }
      is Object -> {
        val obj = JsonObject()
        this.entries.forEach { obj.add(it.key, it.value.toGson()) }
        obj
      }
    }
  }

  fun add(value: JsonValue) {
    if (this is Array) {
      this.values.add(value)
    } else {
      throw UnsupportedOperationException("You can only add single values to Arrays, not $this")
    }
  }

  fun size() = when (this) {
    is Array -> this.values.size
    is Object -> this.entries.size
    else -> 1
  }

  fun type(): String {
    return when (this) {
      is StringValue -> "String"
      else -> this::class.java.simpleName
    }
  }

  fun unwrap(): Any? {
    return when (this) {
      is Null -> null
      is Decimal -> this.value
      is Integer -> this.value
      is StringValue -> this.value
      is True -> true
      is False -> false
      is Array -> this.values
      is Object -> this.entries
    }
  }

  val name: String
    get() {
      return this.javaClass.simpleName
    }

  val isBoolean: Boolean
    get() = when (this) {
      is True, is False -> true
      else -> false
    }

  val isNumber: Boolean
    get() = when (this) {
      is Integer, is Decimal -> true
      else -> false
    }

  val isString: Boolean
    get() = when (this) {
      is StringValue -> true
      else -> false
    }

  val isNull: Boolean
    get() = when (this) {
      is Null -> true
      else -> false
    }
}

fun <R> JsonValue?.map(transform: (JsonValue) -> R): List<R> = when {
  this == null -> emptyList()
  this is JsonValue.Array -> this.values.map(transform)
  else -> emptyList()
}

operator fun JsonValue?.get(field: Any): JsonValue = when {
  this == null -> JsonValue.Null
  else -> this[field]
}

operator fun JsonValue.Object?.get(field: Any): JsonValue = when {
  this == null -> JsonValue.Null
  else -> this[field]
}

fun JsonValue?.orNull() = this ?: JsonValue.Null
