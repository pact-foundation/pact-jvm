package au.com.dius.pact.core.model.matchingrules.expressions

import au.com.dius.pact.core.model.generators.ErrorListener
import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

object MatchingRuleDefinition {
  fun parseMatchingRuleDefinition(expression: String): Result<Triple<String, MatchingRule?, Generator?>, String> {
    val charStream = CharStreams.fromString(expression)
    val lexer = MatcherDefinitionLexer(charStream)
    val tokens = CommonTokenStream(lexer)
    val parser = MatcherDefinitionParser(tokens)
    val errorListener = ErrorListener()
    parser.addErrorListener(errorListener)
    val result = parser.matchingDefinition()
    return if (errorListener.errors.isNotEmpty()) {
      Err("Error parsing expression: ${errorListener.errors.joinToString(", ")}")
    } else {
      Ok(Triple(result.value, result.rule, result.generator))
    }
  }
}
