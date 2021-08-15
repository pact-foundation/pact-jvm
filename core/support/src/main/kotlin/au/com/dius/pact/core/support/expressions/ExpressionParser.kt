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
    fun from(example: Any) = when (example) {
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
  const val END_EXPRESSION = "}"
  const val START_EXP_SYS_PROP = "pact.expressions.start"
  const val END_EXP_SYS_PROP = "pact.expressions.end"

  @JvmOverloads
  @JvmStatic
  fun parseListExpression(
    value: String,
    valueResolver: ValueResolver = SystemPropertyResolver,
    allowReplacement: Boolean = false
  ): List<String> {
    return replaceExpressions(value, valueResolver, allowReplacement)
      .split(VALUES_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
  }

  @JvmOverloads
  @JvmStatic
  fun parseExpression(
    value: String?,
    type: DataType,
    valueResolver: ValueResolver = SystemPropertyResolver,
    allowReplacement: Boolean = false
  ): Any? {
    return when {
      containsExpressions(value, allowReplacement) ->
        type.convert(replaceExpressions(value!!, valueResolver, allowReplacement))
      value != null -> type.convert(value)
      else -> null
    }
  }

  @JvmOverloads
  @JvmStatic
  fun containsExpressions(value: String?, allowReplacement: Boolean = false) =
    value != null && value.contains(startExpression(allowReplacement))

  private fun startExpression(allowReplacement: Boolean) = if (allowReplacement)
    System.getProperty(START_EXP_SYS_PROP).orEmpty().ifEmpty { START_EXPRESSION }
  else
    START_EXPRESSION

  private fun endExpression(allowReplacement: Boolean) = if (allowReplacement)
    System.getProperty(END_EXP_SYS_PROP).orEmpty().ifEmpty { END_EXPRESSION }
  else
    END_EXPRESSION

  private fun replaceExpressions(value: String, valueResolver: ValueResolver, allowReplacement: Boolean): String {
    val joiner = StringJoiner("")

    var buffer = value
    val startExpression = startExpression(allowReplacement)
    val endExpression = endExpression(allowReplacement)
    var position = buffer.indexOf(startExpression)
    while (position >= 0) {
      if (position > 0) {
        joiner.add(buffer.substring(0, position))
      }
      val endPosition = buffer.indexOf(endExpression, position)
      if (endPosition < 0) {
        throw RuntimeException("Missing closing value in expression string \"$value\"")
      }
      var expression = ""
      if (endPosition - position > 2) {
        expression = valueResolver.resolveValue(buffer.substring(position + 2, endPosition)) ?: ""
      }
      joiner.add(expression)
      buffer = buffer.substring(endPosition + endExpression.length)
      position = buffer.indexOf(startExpression)
    }
    joiner.add(buffer)

    return joiner.toString()
  }

  @JvmStatic
  fun toDefaultExpressions(expression: String): String {
    val startExpression = System.getProperty(START_EXP_SYS_PROP)
    val endExpression = System.getProperty(END_EXP_SYS_PROP)
    val updated = if (startExpression.isNullOrEmpty()) {
      expression
    } else {
      expression.replace(startExpression, START_EXPRESSION)
    }
    return if (endExpression.isNullOrEmpty()) {
      updated
    } else {
      updated.replace(endExpression, END_EXPRESSION)
    }
  }
}
