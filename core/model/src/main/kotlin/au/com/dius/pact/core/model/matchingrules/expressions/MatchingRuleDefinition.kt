package au.com.dius.pact.core.model.matchingrules.expressions

import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.support.Either
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.isNotEmpty
import io.github.oshai.kotlinlogging.KLogging

data class MatchingReference(
  val name: String
)

enum class ValueType {
  Unknown,
  String,
  Number,
  Integer,
  Decimal,
  Boolean;

  fun merge(valueType: ValueType): ValueType {
    return when (this) {
      String -> String
      Number -> when (valueType) {
        Number, Boolean, Unknown -> Number
        Integer -> Integer
        Decimal -> Decimal
        String -> String
      }
      Integer -> when (valueType) {
        Number, Integer, Boolean -> Integer
        Decimal -> Decimal
        String -> String
        Unknown -> Integer
      }
      Decimal -> when (valueType) {
        Number, Integer, Boolean -> Decimal
        Decimal -> Decimal
        String -> String
        Unknown -> Decimal
      }
      Boolean -> when (valueType) {
        Number -> Number
        Integer -> Integer
        Decimal -> Decimal
        String -> String
        Unknown, Boolean -> Boolean
      }
      Unknown -> valueType
    }
  }
}

data class MatchingRuleDefinition(
  val value: String?,
  val valueType: ValueType,
  val rules: List<Either<MatchingRule, MatchingReference>>,
  val generator: Generator?
) {
  constructor(
    value: String?,
    rule: MatchingRule?,
    generator: Generator?
  ): this(
    value,
    ValueType.Unknown,
    if (rule != null) listOf(Either.A(rule)) else emptyList(),
    generator
  )

  constructor(
    value: String?,
    rule: MatchingReference,
    generator: Generator?
  ): this(
    value,
    ValueType.Unknown,
    listOf(Either.B(rule)),
    generator
  )

  /**
   * Merges two matching rules definitions. This is used when multiple matching rules are
   * provided for a single element.
   */
  fun merge(other: MatchingRuleDefinition?): MatchingRuleDefinition {
    if (other != null) {
      if (value.isNotEmpty() && other.value.isNotEmpty()) {
        logger.warn {
          "There are multiple matching rules with values for the same value. There is no reliable way to combine " +
            "them, so the later value ('${other.value}') will be ignored."
        }
      }

      if (generator != null && other.generator != null) {
        logger.warn {
          "There are multiple generators for the same value. There is no reliable way to combine them, " +
            "so the later generator (${other.generator}) will be ignored."
        }
      }

      return MatchingRuleDefinition(
        if (value.isNotEmpty()) value else other.value,
        valueType.merge(other.valueType),
        rules + other.rules,
        generator ?: other.generator)
    } else {
      return this
    }
  }

  fun withType(valueType: ValueType): MatchingRuleDefinition {
    return copy(valueType = valueType)
  }

  companion object: KLogging() {
    /**
     * Parse the matching rule expression into a matching rule definition
     */
    @JvmStatic
    fun parseMatchingRuleDefinition(expression: String): Result<MatchingRuleDefinition, String> {
      return if (expression.isEmpty()) {
        Result.Err("Error parsing expression: expression is empty")
      } else {
        val lexer = MatcherDefinitionLexer(expression)
        val parser = MatcherDefinitionParser(lexer)
        when (val result = parser.matchingDefinition()) {
          is Result.Ok -> if (result.value == null) {
            Result.Err("Error parsing expression")
          } else {
            Result.Ok(result.value!!)
          }

          is Result.Err -> Result.Err("Error parsing expression: ${result.error}")
        }
      }
    }
  }
}
