package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.matchers.util.corresponds
import au.com.dius.pact.core.matchers.util.tails
import au.com.dius.pact.core.model.PathToken
import au.com.dius.pact.core.model.matchingrules.EqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.model.parsePath
import au.com.dius.pact.core.support.json.JsonValue
import mu.KLogging
import java.util.Comparator
import java.util.function.Predicate

object Matchers : KLogging() {

  const val PACT_MATCHING_WILDCARD = "pact.matching.wildcard"
  private val intRegex = Regex("\\d+")

  private fun matchesToken(pathElement: String, token: PathToken): Int {
    return when (token) {
      is PathToken.Root -> if (pathElement == "$") 2 else 0
      is PathToken.Field -> if (pathElement == token.name) 2 else 0
      is PathToken.Index -> if (pathElement.matches(intRegex) && token.index == pathElement.toInt()) 2 else 0
      is PathToken.StarIndex -> if (pathElement.matches(intRegex)) 1 else 0
      is PathToken.Star -> 1
      else -> 0
    }
  }

  fun matchesPath(pathExp: String, path: List<String>): Int {
    val parseResult = parsePath(pathExp)
    val filter = tails(path.reversed()).filter { l ->
      corresponds(l.reversed(), parseResult) { pathElement, pathToken ->
        matchesToken(pathElement, pathToken) != 0
      }
    }
    return if (filter.isNotEmpty()) {
      filter.maxByOrNull { seq -> seq.size }?.size ?: 0
    } else {
      0
    }
  }

  fun calculatePathWeight(pathExp: String, path: List<String>): Int {
    val parseResult = parsePath(pathExp)
    return path.zip(parseResult).asSequence().map {
        matchesToken(it.first, it.second)
    }.reduce { acc, i -> acc * i }
  }

  @Deprecated("Use function from MatchingContext or MatchingRuleCategory")
  fun resolveMatchers(
    matchers: MatchingRules,
    category: String,
    items: List<String>,
    pathComparator: Comparator<String>
  ) = if (category == "body")
      matchers.rulesForCategory(category).filter(Predicate {
        matchesPath(it, items) > 0
      })
    else if (category == "header" || category == "query" || category == "metadata")
      matchers.rulesForCategory(category).filter(Predicate { key ->
        items.all { pathComparator.compare(key, it) == 0 }
      })
    else matchers.rulesForCategory(category)

  @Deprecated("Use function from MatchingContext or MatchingRuleCategory")
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
   * Determines if a matcher of the form '[*]' exists for the path
   */
  @Deprecated("Use function from MatchingContext or MatchingRuleCategory")
  @JvmStatic
  fun wildcardIndexMatcherDefined(path: List<String>, category: String, matchers: MatchingRules?) =
    if (matchers != null) {
      val resolvedMatchers = matchers.rulesForCategory(category).filter(Predicate {
        matchesPath(it, path) == path.size
      })
      resolvedMatchers.matchingRules.keys.any { entry -> entry.endsWith("[*]") }
    } else false

  /**
   * Determines if any ignore-order matcher is defined for path or ancestor of path.
   */
  @Deprecated("Use function from MatchingContext or MatchingRuleCategory")
  @JvmStatic
  fun isEqualsIgnoreOrderMatcherDefined(path: List<String>, category: String, matchers: MatchingRules?) =
    if (matchers != null) {
      val matcherDef = selectBestMatcher(matchers, category, path)
      matcherDef.rules.any {
        it is EqualsIgnoreOrderMatcher ||
        it is MinEqualsIgnoreOrderMatcher ||
        it is MaxEqualsIgnoreOrderMatcher ||
        it is MinMaxEqualsIgnoreOrderMatcher
      }
    } else false

  /**
   * Determines if a matcher of the form '.*' exists for the path
   */
  @JvmStatic
  @Deprecated("Use function from MatchingContext or MatchingRuleCategory")
  fun wildcardMatcherDefined(path: List<String>, category: String, matchers: MatchingRules?) =
    if (matchers != null) {
      val resolvedMatchers = matchers.rulesForCategory(category).filter(Predicate {
        matchesPath(it, path) == path.size
      })
      resolvedMatchers.matchingRules.keys.any { entry -> entry.endsWith(".*") }
    } else false

  /**
   * If wildcard matching logic is enabled (where keys are ignored and only values are compared)
   */
  @JvmStatic
  fun wildcardMatchingEnabled() = System.getProperty(PACT_MATCHING_WILDCARD)?.trim() == "true"

  @JvmStatic
  @JvmOverloads
  fun <M : Mismatch> domatch(
    context: MatchingContext,
    path: List<String>,
    expected: Any?,
    actual: Any?,
    mismatchFn: MismatchFactory<M>,
    pathComparator: Comparator<String> = Comparator.naturalOrder()
  ): List<M> {
    val matcherDef = context.selectBestMatcher(path, pathComparator)
    return domatch(matcherDef, path, expected, actual, mismatchFn)
  }

  @Deprecated("Use function from MatchingContext or MatchingRuleCategory")
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

  @Deprecated("Use function from MatchingContext or MatchingRuleCategory")
  fun typeMatcherDefined(category: String, path: List<String>, matchingRules: MatchingRules): Boolean {
    val resolvedMatchers = resolveMatchers(matchingRules, category, path, Comparator.naturalOrder())
    return resolvedMatchers.allMatchingRules().any { it is TypeMatcher }
  }

  /**
   * Compares the expected and actual maps using the provided matching rule
   */
  fun <T> compareMaps(
    path: List<String>,
    matcher: MatchingRule,
    expectedEntries: Map<String, T>,
    actualEntries: Map<String, T>,
    context: MatchingContext,
    generateDiff: () -> String,
    callback: (List<String>, T?, T?) -> List<BodyItemMatchResult>
  ): List<BodyItemMatchResult> {
    val result = mutableListOf<BodyItemMatchResult>()
    if (wildcardMatchingEnabled() && context.wildcardMatcherDefined(path + "any")) {
      actualEntries.entries.forEach { (key, value) ->
        if (expectedEntries.containsKey(key)) {
          result.addAll(callback(path + key, expectedEntries[key]!!, value))
        } else {
          result.addAll(callback(path + key, expectedEntries.values.firstOrNull(), value))
        }
      }
    } else {
      result.addAll(context.matchKeys(path, expectedEntries, actualEntries, generateDiff))
      expectedEntries.entries.forEach { (key, value) ->
        if (actualEntries.containsKey(key)) {
          result.addAll(callback(path + key, value, actualEntries[key]))
        }
      }
    }
    return result
  }
}
