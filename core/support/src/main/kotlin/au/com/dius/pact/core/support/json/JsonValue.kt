package au.com.dius.pact.core.support.json

import au.com.dius.pact.core.support.Json

sealed class JsonValue {
  class Integer(val value: JsonToken.Integer) : JsonValue() {
    constructor(value: CharArray) : this(JsonToken.Integer(value))
    fun toBigInteger() = String(this.value.chars).toBigInteger()
  }

  class Decimal(val value: JsonToken.Decimal) : JsonValue() {
    constructor(value: CharArray) : this(JsonToken.Decimal(value))
    fun toBigDecimal() = String(this.value.chars).toBigDecimal()
  }

  class StringValue(val value: JsonToken.StringValue) : JsonValue() {
    constructor(value: CharArray) : this(JsonToken.StringValue(value))
    override fun toString() = String(value.chars)
  }
  object True : JsonValue()
  object False : JsonValue()
  object Null : JsonValue()

  class Array(val values: MutableList<JsonValue> = mutableListOf()) : JsonValue() {
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

  class Object(val entries: MutableMap<String, JsonValue>) : JsonValue() {
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

  fun asString(): String? {
    return if (this is StringValue) {
      String(value.chars)
    } else {
      null
    }
  }

  override fun toString(): String {
    return when (this) {
      is Null -> "null"
      is Decimal -> String(this.value.chars)
      is Integer -> String(this.value.chars)
      is StringValue -> this.value.toString()
      is True -> "true"
      is False -> "false"
      is Array -> "[${this.values.joinToString(",")}]"
      is Object -> "{${this.entries.entries.sortedBy { it.key }.joinToString(",") { "\"${it.key}\":" + it.value }}}"
    }
  }

  fun asBoolean() = when (this) {
    is True -> true
    is False -> false
    else -> throw UnsupportedOperationException("Expected a Boolean, but found a $this")
  }

  fun asNumber(): Number = when (this) {
    is Integer -> this.toBigInteger()
    is Decimal -> this.toBigDecimal()
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
      is Decimal -> String(this.value.chars)
      is Integer -> String(this.value.chars)
      is StringValue -> "\"${Json.escape(this.asString()!!)}\""
      is True -> "true"
      is False -> "false"
      is Array -> "[${this.values.joinToString(",") { it.serialise() }}]"
      is Object -> "{${this.entries.entries.sortedBy { it.key }.joinToString(",") { "\"${it.key}\":" + it.value.serialise() }}}"
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
      is Decimal -> this.toBigDecimal()
      is Integer -> this.toBigInteger()
      is StringValue -> this.asString()
      is True -> true
      is False -> false
      is Array -> this.values
      is Object -> this.entries
    }
  }

  override fun equals(other: Any?): Boolean {
    if (other !is JsonValue) return false
    return when (this) {
      is Null -> other is Null
      is Decimal -> other is Decimal && this.toBigDecimal() == other.toBigDecimal()
      is Integer -> other is Integer && this.toBigInteger() == other.toBigInteger()
      is StringValue -> other is StringValue && this.asString() == other.asString()
      is True -> other is True
      is False -> other is False
      is Array -> other is Array && this.values == other.values
      is Object -> other is Object && this.entries == other.entries
    }
  }

  override fun hashCode() = when (this) {
    is Null -> 0.hashCode()
    is Decimal -> this.toBigDecimal().hashCode()
    is Integer -> this.toBigInteger().hashCode()
    is StringValue -> this.asString()!!.hashCode()
    is True -> true.hashCode()
    is False -> false.hashCode()
    is Array -> this.values.hashCode()
    is Object -> this.entries.hashCode()
  }

  fun prettyPrint(indent: Int = 0, skipIndent: Boolean = false): String {
    val indentStr = "".padStart(indent)
    val indentStr2 = "".padStart(indent + 2)
    return if (skipIndent) {
      when (this) {
        is Array -> "[\n" + this.values.joinToString(",\n") {
          it.prettyPrint(indent + 2) } + "\n$indentStr]"
        is Object -> "{\n" + this.entries.entries.sortedBy { it.key }.joinToString(",\n") {
          "$indentStr2\"${it.key}\": ${it.value.prettyPrint(indent + 2, true)}"
          } + "\n$indentStr}"
        else -> this.serialise()
      }
    } else {
      when (this) {
        is Array -> "$indentStr$indentStr[\n" + this.values.joinToString(",\n") {
          it.prettyPrint(indent + 2) } + "\n$indentStr]"
        is Object -> "$indentStr{\n" + this.entries.entries.sortedBy { it.key }.joinToString(",\n") {
          "$indentStr2\"${it.key}\": ${it.value.prettyPrint(indent + 2, true)}"
          } + "\n$indentStr}"
        else -> indentStr + this.serialise()
      }
    }
  }

  val name: String
    get() {
      return when (this) {
        is Null -> "Null"
        is Decimal -> "Decimal"
        is Integer -> "Integer"
        is StringValue -> "String"
        is True -> "True"
        is False -> "False"
        is Array -> "Array"
        is Object -> "Object"
      }
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
