package au.com.dius.pact.core.support.expressions

import java.util.StringJoiner

object ExpressionParser {

  const val VALUES_SEPARATOR = ","
  const val START_EXPRESSION = "\${"
  const val END_EXPRESSION = '}'

  @JvmOverloads
  @JvmStatic
  fun parseListExpression(value: String, valueResolver: ValueResolver = SystemPropertyResolver()): List<String> {
    return replaceExpressions(value, valueResolver).split(VALUES_SEPARATOR).filter { it.isNotEmpty() }
  }

  @JvmOverloads
  @JvmStatic
  fun parseExpression(value: String?, valueResolver: ValueResolver = SystemPropertyResolver()): String? {
    return if (containsExpressions(value)) {
      replaceExpressions(value!!, valueResolver)
    } else value
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
