package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.matchers.Matchers.compareListContent
import au.com.dius.pact.core.matchers.Matchers.compareLists
import au.com.dius.pact.core.matchers.util.IndicesCombination
import au.com.dius.pact.core.matchers.util.LargestKeyValue
import au.com.dius.pact.core.matchers.util.memoizeFixed
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import mu.KLogging
import java.math.BigInteger

object JsonBodyMatcher : BodyMatcher, KLogging() {

  override fun matchBody(
    expected: OptionalBody,
    actual: OptionalBody,
    context: MatchingContext
  ): BodyMatchResult {
    return when {
      expected.isMissing() -> BodyMatchResult(null, emptyList())
      expected.isEmpty() && actual.isEmpty() -> BodyMatchResult(null, emptyList())
      !expected.isEmpty() && actual.isEmpty() ->
        BodyMatchResult(null, listOf(BodyItemMatchResult("$",
          listOf(BodyMismatch(null, actual.valueAsString(), "Expected empty body but received '${actual.value}'")))))
      expected.isNull() && actual.isPresent() ->
        BodyMatchResult(null, listOf(BodyItemMatchResult("$",
          listOf(BodyMismatch(null, actual.valueAsString(), "Expected null body but received '${actual.value}'")))))
      expected.isNull() -> BodyMatchResult(null, emptyList())
      actual.isMissing() ->
        BodyMatchResult(null, listOf(BodyItemMatchResult("$",
          listOf(BodyMismatch(expected.valueAsString(), null, "Expected body '${expected.value}' but was missing")))))
      else -> {
        BodyMatchResult(null, compare(listOf("$"), JsonParser.parseString(expected.valueAsString()),
          JsonParser.parseString(actual.valueAsString()), context))
      }
    }
  }

  private fun valueOf(value: Any?) = when (value) {
    is String -> "'$value'"
    is JsonValue.StringValue -> "'${value.asString()}'"
    is JsonValue -> value.serialise()
    null -> "null"
    else -> value.toString()
  }

  private fun typeOf(value: Any?) = when {
    value is Map<*, *> -> "Map"
    value is JsonValue.Object -> "Map"
    value is List<*> -> "List"
    value is JsonValue.Array -> "List"
    value is JsonValue.Null -> "Null"
    value is JsonValue -> value.name
    value == null -> "Null"
    else -> value.javaClass.simpleName
  }

  fun compare(
    path: List<String>,
    expected: JsonValue,
    actual: JsonValue,
    context: MatchingContext
  ): List<BodyItemMatchResult> {
    return when {
      expected is JsonValue.Object && actual is JsonValue.Object ->
        compareMaps(expected, actual, path, context)
      expected is JsonValue.Array && actual is JsonValue.Array ->
        compareLists(expected, actual, path, context)
      expected is JsonValue.Object && actual !is JsonValue.Object ||
        expected is JsonValue.Array && actual !is JsonValue.Array ->
        listOf(BodyItemMatchResult(path.joinToString("."),
          listOf(BodyMismatch(expected, actual, "Type mismatch: Expected ${typeOf(expected)} " +
          "${valueOf(expected)} but received ${typeOf(actual)} ${valueOf(actual)}", path.joinToString("."),
          generateJsonDiff(expected, actual)))))
      else -> compareValues(path, expected, actual, context)
    }
  }

  private fun compareLists(
    expectedValues: JsonValue.Array,
    actualValues: JsonValue.Array,
    path: List<String>,
    context: MatchingContext
  ): List<BodyItemMatchResult> {
    val expectedList = expectedValues.values
    val actualList = actualValues.values
    val result = mutableListOf<BodyItemMatchResult>()
    val generateDiff = { generateJsonDiff(expectedValues, actualValues) }
    if (context.matcherDefined(path)) {
      logger.debug { "compareLists: Matcher defined for path $path" }
      for (matcher in context.selectBestMatcher(path).rules) {
        result.addAll(compareLists(path, matcher, expectedList, actualList, context, generateDiff) {
          p, expected, actual, context -> compare(p, expected, actual, context)
        })
      }
    } else {
      if (expectedList.isEmpty() && actualList.isNotEmpty()) {
        result.add(BodyItemMatchResult(path.joinToString("."),
          listOf(BodyMismatch(expectedValues, actualValues,
            "Expected an empty List but received ${valueOf(actualValues)}",
          path.joinToString("."), generateJsonDiff(expectedValues, actualValues)))))
      } else {
        result.addAll(compareListContent(expectedList, actualList, path, context, generateDiff) {
          p, expected, actual, context -> compare(p, expected, actual, context)
        })
        if (expectedList.size != actualList.size) {
          result.add(BodyItemMatchResult(path.joinToString("."), listOf(BodyMismatch(expectedList, actualList,
            "Expected a List with ${expectedList.size} elements but received ${actualList.size} elements",
            path.joinToString("."), generateJsonDiff(expectedValues, actualValues)))))
        }
      }
    }
    return result
  }

  private fun compareMaps(
    expectedValues: JsonValue.Object,
    actualValues: JsonValue.Object,
    path: List<String>,
    context: MatchingContext
  ): List<BodyItemMatchResult> {
    return if (expectedValues.isEmpty() && actualValues.isNotEmpty() && !context.allowUnexpectedKeys) {
      listOf(BodyItemMatchResult(path.joinToString("."),
        listOf(BodyMismatch(expectedValues, actualValues, "Expected an empty Map but received ${valueOf(actualValues)}",
        path.joinToString("."), generateJsonDiff(expectedValues, actualValues)))))
    } else {
      val result = mutableListOf<BodyItemMatchResult>()
      val generateDiff = { generateJsonDiff(expectedValues, actualValues) }
      val expectedEntries = expectedValues.entries
      val actualEntries = actualValues.entries
      if (context.matcherDefined(path)) {
        for (matcher in context.selectBestMatcher(path).rules) {
          result.addAll(Matchers.compareMaps(path, matcher, expectedEntries, actualEntries, context, generateDiff) {
            p, expected, actual -> compare(p, expected ?: JsonValue.Null, actual ?: JsonValue.Null, context)
          })
        }
      } else {
        result.addAll(context.matchKeys(path, expectedEntries, actualEntries, generateDiff))
        for ((key, value) in expectedEntries) {
          val p = path + key
          if (actualEntries.containsKey(key)) {
            result.addAll(compare(p, value, actualEntries[key]!!, context))
          }
        }
      }
      result
    }
  }

  private fun compareValues(
    path: List<String>,
    expected: JsonValue,
    actual: JsonValue,
    context: MatchingContext
  ): List<BodyItemMatchResult> {
    return if (context.matcherDefined(path)) {
      logger.debug { "compareValues: Matcher defined for path $path" }
      listOf(BodyItemMatchResult(path.joinToString("."),
        Matchers.domatch(context, path, expected, actual, BodyMismatchFactory)))
    } else {
      logger.debug { "compareValues: No matcher defined for path $path, using equality" }
      if (expected == actual) {
        listOf(BodyItemMatchResult(path.joinToString("."), emptyList()))
      } else {
        listOf(BodyItemMatchResult(path.joinToString("."),
          listOf(BodyMismatch(expected, actual, "Expected ${valueOf(expected)} (${typeOf(expected)}) " +
            "but received ${valueOf(actual)} (${typeOf(actual)})", path.joinToString(".")))))
      }
    }
  }
}
