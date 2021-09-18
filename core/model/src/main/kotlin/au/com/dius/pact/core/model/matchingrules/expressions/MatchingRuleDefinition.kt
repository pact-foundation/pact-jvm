package au.com.dius.pact.core.model.matchingrules.expressions

import au.com.dius.pact.core.model.generators.ErrorListener
import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.support.isNotEmpty
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import mu.KLogging
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

data class MatchingRuleDefinition(
  val value: String?,
  val rules: List<MatchingRule>,
  val generator: Generator?
) {
  constructor(
    value: String?,
    rule: MatchingRule?,
    generator: Generator?
  ): this(
    value,
    if (rule != null) listOf(rule) else emptyList(),
    generator
  )

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

      return MatchingRuleDefinition(if (value.isNotEmpty()) value else other.value, rules + other.rules,
        generator ?: other.generator)
    } else {
      return this
    }
  }

  companion object: KLogging() {
    /**
     * Parse the matching rule expression into a matching rule definition
     */
    @JvmStatic
    fun parseMatchingRuleDefinition(expression: String): Result<MatchingRuleDefinition, String> {
      val charStream = CharStreams.fromString(expression)
      val lexer = MatcherDefinitionLexer(charStream)
      val tokens = CommonTokenStream(lexer)
      val parser = MatcherDefinitionParser(tokens)
      val errorListener = ErrorListener()
      parser.addErrorListener(errorListener)
      val result = parser.matchingDefinition()
      return if (errorListener.errors.isNotEmpty()) {
        Err("Error parsing expression: ${errorListener.errors.joinToString(", ")}")
      } else if (result.value == null) {
        Err("Error parsing expression")
      } else {
        Ok(result.value)
      }
    }
  }
}
