package au.com.dius.pact.matchers

import au.com.dius.pact.matchers.util.corresponds
import au.com.dius.pact.matchers.util.tails
import au.com.dius.pact.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.model.matchingrules.MatchingRules
import io.gatling.jsonpath.AST
import io.gatling.jsonpath.Parser
import mu.KLogging
import scala.collection.JavaConversions
import scala.collection.JavaConverters
import scala.util.parsing.combinator.Parsers
import java.util.function.Predicate
import java.util.function.ToIntFunction

object Matchers : KLogging() {

  fun matchesToken(pathElement: String, token: AST.PathToken) = when (token) {
    is AST.`RootNode$` -> if (pathElement == "$") 2 else 0
    is AST.Field -> if (pathElement == token.name()) 2 else 0
    is AST.ArrayRandomAccess -> if (pathElement.matches(Regex("\\d+")) && token.indices().contains(pathElement.toInt())) 2 else 0
    is AST.ArraySlice -> if (pathElement.matches(Regex("\\d+"))) 1 else 0
    is AST.`AnyField$` -> 1
    else -> 0
  }

  fun matchesPath(pathExp: String, path: List<String>): Int {
    val parseResult = Parser().compile(pathExp)
    return when (parseResult) {
      is Parsers.Success -> {
        val filter = tails(path.reversed()).filter { l ->
          corresponds(l.reversed(), JavaConversions.asJavaCollection(parseResult.result()).toList()) { pathElement, pathToken ->
            matchesToken(pathElement, pathToken) != 0
          }
        }
        if (filter.isNotEmpty()) {
          filter.maxBy { seq -> seq.size }?.size ?: 0
        } else {
          0
        }
      }
      else -> {
        logger.warn { "Path expression $pathExp is invalid, ignoring: $parseResult" }
        0
      }
    }
  }

  fun calculatePathWeight(pathExp: String, path: List<String>): Int {
    val parseResult = Parser().compile(pathExp)
    return when (parseResult) {
      is Parsers.Success -> path.zip(JavaConverters.asJavaCollectionConverter(parseResult.result()).asJavaCollection()).map {
        matchesToken(it.first, it.second)
      }.reduce { acc, i -> acc * i }
      else -> {
        logger.warn { "Path expression $pathExp is invalid, ignoring: $parseResult" }
        0
      }
    }
  }

  fun resolveMatchers(matchers: MatchingRules, category: String, items: List<String>) =
    if (category == "body")
      matchers.rulesForCategory(category).filter(Predicate {
        matchesPath(it, items) > 0
      })
    else if (category == "header" || category == "query")
      matchers.rulesForCategory(category).filter(Predicate {
        items == listOf(it)
      })
    else matchers.rulesForCategory(category)

  @JvmStatic
  fun matcherDefined(category: String, path: List<String>, matchers: MatchingRules?): Boolean =
    if (matchers != null)
      resolveMatchers(matchers, category, path).isNotEmpty()
    else
      false

  @JvmStatic
  fun wildcardMatcherDefined(path: List<String>, category: String, matchers: MatchingRules?) =
    if (matchers != null) {
      val resolvedMatchers = matchers.rulesForCategory(category).filter(Predicate {
        matchesPath(it, path) == path.size
      })
      resolvedMatchers.matchingRules.keys.any { entry -> entry.endsWith(".*") }
    } else {
      false
    }

  @JvmStatic
  fun <M: Mismatch> domatch(matchers: MatchingRules, category: String, path: List<String>, expected: Any?, actual: Any?,
                        mismatchFn: MismatchFactory<M>) : List<M> {
    val matcherDef = selectBestMatcher(matchers, category, path)
    return domatch(matcherDef, path, expected, actual, mismatchFn)
  }

  @JvmStatic
  fun selectBestMatcher(matchers: MatchingRules, category: String, path: List<String>): MatchingRuleGroup {
    val matcherCategory = resolveMatchers(matchers, category, path)
    return if (category == "body")
      matcherCategory.maxBy(ToIntFunction {
        calculatePathWeight(it, path)
      })
    else {
      matcherCategory.matchingRules.values.first()
    }
  }
}
