package au.com.dius.pact.core.support.expressions

import java.lang.Double.parseDouble
import java.lang.Long.parseLong
import java.math.BigDecimal
import java.math.BigInteger
import java.util.StringJoiner

enum class DataType {
  STRING,
  INTEGER,
  DECIMAL,
  FLOAT,
  RAW,
  BOOLEAN;

  fun convert(value: Any) = when (this) {
    INTEGER -> if (value is Number) value.toLong() else parseLong(value.toString())
    DECIMAL -> BigDecimal(value.toString())
    FLOAT -> if (value is Number) value.toDouble() else parseDouble(value.toString())
    STRING -> value.toString()
    BOOLEAN -> value.toString() == "true"
    else -> value
  }

  companion object {
    @JvmStatic
    fun from(example: Any?) = when (example) {
      is Int -> INTEGER
      is Long -> INTEGER
      is BigInteger -> INTEGER
      is Float -> FLOAT
      is Double -> FLOAT
      is BigDecimal -> DECIMAL
      is String -> STRING
      is Boolean -> BOOLEAN
      else -> RAW
    }
  }
}

object ExpressionParser {

  const val VALUES_SEPARATOR = ","
  const val START_EXPRESSION = "\${"
  const val END_EXPRESSION = '}'

  @JvmOverloads
  @JvmStatic
  fun parseListExpression(value: String, valueResolver: ValueResolver = SystemPropertyResolver): List<String> {
    return replaceExpressions(value, valueResolver).split(VALUES_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
  }

  @JvmOverloads
  @JvmStatic
  fun parseExpression(value: String?, type: DataType, valueResolver: ValueResolver = SystemPropertyResolver): Any? {
    return when {
      containsExpressions(value) -> type.convert(replaceExpressions(value!!, valueResolver))
      value != null -> type.convert(value)
      else -> null
    }
  }

  fun containsExpressions(value: String?) = value != null && value.contains(START_EXPRESSION)

  private fun replaceExpressions(value: String, valueResolver: ValueResolver): String {
    val joiner = StringJoiner("")

    var buffer = value
    var position = buffer.indexOf(START_EXPRESSION)
    while (position >= 0) {
      if (position > 0) {
        joiner.add(buffer.substring(0, position))
      }
      val endPosition = buffer.indexOf(END_EXPRESSION, position)
      if (endPosition < 0) {
        throw RuntimeException("Missing closing brace in expression string \"$value]\"")
      }
      var expression = ""
      if (endPosition - position > 2) {
        expression = valueResolver.resolveValue(buffer.substring(position + 2, endPosition)) ?: ""
      }
      joiner.add(expression)
      buffer = buffer.substring(endPosition + 1)
      position = buffer.indexOf(START_EXPRESSION)
    }
    joiner.add(buffer)

    return joiner.toString()
  }
}
