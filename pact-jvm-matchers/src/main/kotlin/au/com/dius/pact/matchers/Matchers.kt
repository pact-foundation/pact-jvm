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
import java.util.Comparator
import java.util.function.Predicate

object Matchers : KLogging() {

  const val PACT_MATCHING_WILDCARD = "pact.matching.wildcard"

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

  fun resolveMatchers(
    matchers: MatchingRules,
    category: String,
    items: List<String>,
    pathComparator: Comparator<String>
  ) = if (category == "body")
      matchers.rulesForCategory(category).filter(Predicate {
        matchesPath(it, items) > 0
      })
    else if (category == "header" || category == "query")
      matchers.rulesForCategory(category).filter(Predicate { key ->
        items.all { pathComparator.compare(key, it) == 0 }
      })
    else matchers.rulesForCategory(category)

  @JvmStatic
  @JvmOverloads
  fun matcherDefined(
    category: String,
    path: List<String>,
    matchers: MatchingRules?,
    pathComparator: Comparator<String> = Comparator.naturalOrder()
  ): Boolean =
    if (matchers != null)
      resolveMatchers(matchers, category, path, pathComparator).isNotEmpty()
    else false

  /**
   * Determines if a matcher of the form '.*' exists for the path
   */
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

  /**
   * If wildcard matching logic is enabled (where keys are ignored and only values are compared)
   */
  @JvmStatic
  fun wildcardMatchingEnabled() = System.getProperty(PACT_MATCHING_WILDCARD)?.trim() == "true"

  @JvmStatic
  @JvmOverloads
  fun <M : Mismatch> domatch(
    matchers: MatchingRules,
    category: String,
    path: List<String>,
    expected: Any?,
    actual: Any?,
    mismatchFn: MismatchFactory<M>,
    pathComparator: Comparator<String> = Comparator.naturalOrder()
  ): List<M> {
    val matcherDef = selectBestMatcher(matchers, category, path, pathComparator)
    return domatch(matcherDef, path, expected, actual, mismatchFn)
  }

  @JvmStatic
  @JvmOverloads
  fun selectBestMatcher(
    matchers: MatchingRules,
    category: String,
    path: List<String>,
    pathComparator: Comparator<String> = Comparator.naturalOrder()
  ): MatchingRuleGroup {
    val matcherCategory = resolveMatchers(matchers, category, path, pathComparator)
    return if (category == "body")
      matcherCategory.maxBy(Comparator { a, b ->
        val weightA = calculatePathWeight(a, path)
        val weightB = calculatePathWeight(b, path)
        when {
          weightA == weightB -> when {
            a.length > b.length -> 1
            a.length < b.length -> -1
            else -> 0
          }
          weightA > weightB -> 1
          else -> -1
        }
      })
    else {
      matcherCategory.matchingRules.values.first()
    }
  }
}
