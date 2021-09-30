package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.matchers.util.IndicesCombination
import au.com.dius.pact.core.matchers.util.LargestKeyValue
import au.com.dius.pact.core.matchers.util.corresponds
import au.com.dius.pact.core.matchers.util.memoizeFixed
import au.com.dius.pact.core.matchers.util.padTo
import au.com.dius.pact.core.matchers.util.tails
import au.com.dius.pact.core.model.PathToken
import au.com.dius.pact.core.model.constructPath
import au.com.dius.pact.core.model.matchingrules.ArrayContainsMatcher
import au.com.dius.pact.core.model.matchingrules.EachKeyMatcher
import au.com.dius.pact.core.model.matchingrules.EachValueMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.model.matchingrules.ValuesMatcher
import au.com.dius.pact.core.model.parsePath
import mu.KLogging
import java.math.BigInteger
import java.util.Comparator
import java.util.function.Predicate

@Suppress("TooManyFunctions")
object Matchers : KLogging() {

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
    if (matcher is ValuesMatcher || matcher is EachValueMatcher) {
      actualEntries.entries.forEach { (key, value) ->
        if (expectedEntries.containsKey(key)) {
          result.addAll(callback(path + key, expectedEntries[key]!!, value))
        } else {
          result.addAll(callback(path + key, expectedEntries.values.firstOrNull(), value))
        }
      }
    } else {
      result.addAll(context.matchKeys(path, expectedEntries, actualEntries, generateDiff))
      if (matcher !is EachKeyMatcher) {
        expectedEntries.entries.forEach { (key, value) ->
          if (actualEntries.containsKey(key)) {
            result.addAll(callback(path + key, value, actualEntries[key]))
          }
        }
      }
    }
    return result
  }

  @Suppress("LongMethod")
  fun <T> compareLists(
    path: List<String>,
    matcher: MatchingRule,
    expectedList: List<T>,
    actualList: List<T>,
    context: MatchingContext,
    generateDiff: () -> String,
    cascaded: Boolean,
    callback: (List<String>, T, T, MatchingContext) -> List<BodyItemMatchResult>
  ): List<BodyItemMatchResult> {
    val result = mutableListOf<BodyItemMatchResult>()
    val matchResult = domatch(matcher, path, expectedList, actualList, BodyMismatchFactory, cascaded)
    if (matchResult.isNotEmpty()) {
      result.add(BodyItemMatchResult(constructPath(path), matchResult))
    }
    if (expectedList.isNotEmpty()) {
      when (matcher) {
        is EqualsIgnoreOrderMatcher,
        is MinEqualsIgnoreOrderMatcher,
        is MaxEqualsIgnoreOrderMatcher,
        is MinMaxEqualsIgnoreOrderMatcher -> {
          // match unordered list
          logger.debug { "compareLists: ignore-order matcher defined for path $path" }
          // No need to pad 'expected' as we already visit all 'actual' values
          result.addAll(compareListContentUnordered(expectedList, actualList, path, context, generateDiff, callback))
        }
        is ArrayContainsMatcher -> {
          val variants = matcher.variants.ifEmpty {
            expectedList.mapIndexed { index, _ ->
              Triple(
                index,
                MatchingRuleCategory("body", mutableMapOf("" to MatchingRuleGroup(mutableListOf(EqualsMatcher)))),
                emptyMap()
              )
            }
          }
          for ((index, variant) in variants.withIndex()) {
            if (index < expectedList.size) {
              val expectedValue = expectedList[index]
              val newContext = MatchingContext(variant.second, context.allowUnexpectedKeys, context.pluginConfiguration)
              val noneMatched = actualList.withIndex().all { (actualIndex, value) ->
                val variantResult = callback(listOf("$"), expectedValue, value, newContext)
                val mismatches = variantResult.flatMap { it.result }
                logger.debug {
                  "Comparing list item $actualIndex with value '$value' to '$expectedValue' -> " +
                    "${mismatches.size} mismatches"
                }
                mismatches.isNotEmpty()
              }
              if (noneMatched) {
                result.add(BodyItemMatchResult(constructPath(path),
                  listOf(BodyMismatch(expectedValue, actualList,
                    "Variant at index $index ($expectedValue) was not found in the actual list",
                    constructPath(path), generateDiff()
                  ))
                ))
              }
            } else {
              result.add(BodyItemMatchResult(constructPath(path),
                listOf(BodyMismatch(expectedList, actualList,
                  "ArrayContains: variant $index is missing from the expected list, which has " +
                    "${expectedList.size} items", constructPath(path), generateDiff()
                ))
              ))
            }
          }
        }
        else -> {
          result.addAll(compareListContent(expectedList.padTo(actualList.size, expectedList.first()),
            actualList, path, context, generateDiff, callback))
        }
      }
    }
    return result
  }

  /**
   * Compares any "extra" actual elements to expected
   */
  private fun <T> compareActualElements(
    path: List<String>,
    actualIndex: Int,
    expectedValues: List<T>,
    actual: T,
    context: MatchingContext,
    callback: (List<String>, T, T, MatchingContext) -> List<BodyItemMatchResult>
  ): List<BodyItemMatchResult> {
    val indexPath = path + actualIndex.toString()
    return if (context.directMatcherDefined(indexPath)) {
      callback(indexPath, expectedValues[0], actual, context)
    } else {
      emptyList()
    }
  }

  /**
   * Compares every permutation of actual against expected.
   */
  fun <T> compareListContentUnordered(
    expectedList: List<T>,
    actualList: List<T>,
    path: List<String>,
    context: MatchingContext,
    generateDiff: () -> String,
    callback: (List<String>, T, T, MatchingContext) -> List<BodyItemMatchResult>
  ): List<BodyItemMatchResult> {
    val memoizedActualCompare = { actualIndex: Int ->
      compareActualElements(path, actualIndex, expectedList, actualList[actualIndex], context, callback)
    }.memoizeFixed(actualList.size)

    val memoizedCompare = { expectedIndex: Int, actualIndex: Int ->
      callback(path + expectedIndex.toString(), expectedList[expectedIndex], actualList[actualIndex], context)
    }.memoizeFixed(expectedList.size, actualList.size)

    val longestMatch = LargestKeyValue<Int, IndicesCombination>()
    val examinedActualIndicesCombos = HashSet<BigInteger>()
    /**
     * Determines if a matching permutation exists.
     *
     * Note: this algorithm seems to have a worst case O(n*2^n) time and O(2^n) space complexity
     * if a lot of the actuals match a lot of the expected (e.g., if expected uses regex matching
     * with something like [3|2|1, 2|1, 1]). Without the caching, the time complexity jumps to
     * around O(2^2n). Caching/memoization is also used above for compare, to effectively achieve
     * just O(n^2) calls instead of O(2^n).
     *
     * For most normal cases, average performance should be closer to O(n^2) time and O(n) space
     * complexity if there aren't many duplicate matches. Best case is O(n)/O(n) if its already
     * in order.
     *
     * @param expectedIndex index of expected being compared against
     * @param actualIndices combination of remaining actual indices to compare
     */
    fun hasMatchingPermutation(
      expectedIndex: Int = 0,
      actualIndices: IndicesCombination = IndicesCombination.of(actualList.size)
    ): Boolean {
      return if (actualIndices.comboId in examinedActualIndicesCombos) {
        false
      } else {
        examinedActualIndicesCombos.add(actualIndices.comboId)
        longestMatch.useIfLarger(expectedIndex, actualIndices)
        when {
          expectedIndex < expectedList.size -> {
            actualIndices.indices().any { actualIndex ->
              memoizedCompare(expectedIndex, actualIndex).all {
                it.result.isEmpty()
              } && hasMatchingPermutation(expectedIndex + 1, actualIndices - actualIndex)
            }
          }
          actualList.size > expectedList.size -> {
            actualIndices.indices().all { actualIndex ->
              memoizedActualCompare(actualIndex).all {
                it.result.isEmpty()
              }
            }
          }
          else -> true
        }
      }
    }

    return if (hasMatchingPermutation()) {
      emptyList()
    } else {
      val smallestCombo = longestMatch.value ?: IndicesCombination.of(actualList.size)
      val longestMatch = longestMatch.key ?: 0

      val remainingErrors = smallestCombo.indices().map { actualIndex ->
        (longestMatch until expectedList.size).map { expectedIndex ->
          memoizedCompare(expectedIndex, actualIndex).flatMap { it.result }
        }.flatten() + if (actualList.size > expectedList.size) {
          memoizedActualCompare(actualIndex).flatMap { it.result }
        } else emptyList()
      }.toList().flatten()
        .groupBy { it.path }
        .map { (path, mismatches) -> BodyItemMatchResult(path, mismatches) }

      listOf(BodyItemMatchResult(constructPath(path),
        listOf(BodyMismatch(expectedList, actualList,
          "Expected $expectedList to match $actualList ignoring order of elements",
          constructPath(path), generateDiff()
        ))
      )) + remainingErrors
    }
  }

  fun <T> compareListContent(
    expectedList: List<T>,
    actualList: List<T>,
    path: List<String>,
    context: MatchingContext,
    generateDiff: () -> String,
    callback: (List<String>, T, T, MatchingContext) -> List<BodyItemMatchResult>
  ): List<BodyItemMatchResult> {
    val result = mutableListOf<BodyItemMatchResult>()
    for ((index, value) in expectedList.withIndex()) {
      if (index < actualList.size) {
        result.addAll(callback(path + index.toString(), value, actualList[index], context))
      } else if (!context.matcherDefined(path)) {
        result.add(BodyItemMatchResult(constructPath(path),
          listOf(BodyMismatch(expectedList, actualList,
            "Expected $value but was missing",
            constructPath(path), generateDiff()))))
      }
    }
    return result
  }
}
