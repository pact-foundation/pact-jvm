package au.com.dius.pact.core.support.json

import au.com.dius.pact.core.support.Json

sealed class JsonBuilder(open val root: JsonValue) {
  class JsonObjectBuilder(override val root: JsonValue.Object = JsonValue.Object()): JsonBuilder(root) {
    operator fun set(name: String, value: Any?) {
      root[name] = Json.toJson(value)
    }

    fun `object`(function: (JsonObjectBuilder) -> Unit): JsonValue.Object {
      return build(function)
    }

    fun array(function: (JsonArrayBuilder) -> Unit): JsonValue.Array {
      val builder = JsonArrayBuilder()
      function(builder)
      return builder.root
    }
  }

  class JsonArrayBuilder(override val root: JsonValue.Array = JsonValue.Array()): JsonBuilder(root) {
    operator fun set(i: Int, value: Any?) {
      root[i] = Json.toJson(value)
    }

    fun push(value: Any?) {
      root.append(Json.toJson(value))
    }

    operator fun plusAssign(value: Any?) {
      push(value)
    }

    fun `object`(function: (JsonObjectBuilder) -> Unit): JsonValue.Object {
      return build(function)
    }
  }

  companion object {
    fun build(function: (JsonObjectBuilder) -> Unit): JsonValue.Object {
      val builder = JsonObjectBuilder()
      function(builder)
      return builder.root
    }
  }
}
