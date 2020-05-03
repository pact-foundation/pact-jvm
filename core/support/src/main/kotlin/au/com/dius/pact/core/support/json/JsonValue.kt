package au.com.dius.pact.core.support.json

import java.math.BigDecimal
import java.math.BigInteger

sealed class JsonValue {
  data class Integer(val value: BigInteger) : JsonValue()
  data class Decimal(val value: BigDecimal) : JsonValue()
  data class StringValue(val value: String) : JsonValue()
  object True : JsonValue()
  object False : JsonValue()
  object Null : JsonValue()
  data class Array(val values: MutableList<JsonValue>) : JsonValue()
  data class Object(val entries: MutableMap<String, JsonValue>) : JsonValue()
}

val JsonValue.name: String
  get() {
    return this.javaClass.simpleName
  }
