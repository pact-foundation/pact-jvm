package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.matchers.util.padTo
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import au.com.dius.pact.core.support.jsonArray
import mu.KLogging

object JsonBodyMatcher : BodyMatcher, KLogging() {

  override fun matchBody(
    expected: OptionalBody,
    actual: OptionalBody,
    allowUnexpectedKeys: Boolean,
    matchingRules: MatchingRules
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
          JsonParser.parseString(actual.valueAsString()), allowUnexpectedKeys, matchingRules))
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

  private fun compare(
    path: List<String>,
    expected: JsonValue,
    actual: JsonValue,
    allowUnexpectedKeys: Boolean,
    matchers: MatchingRules
  ): List<BodyItemMatchResult> {
    return when {
      expected is JsonValue.Object && actual is JsonValue.Object ->
        compareMaps(expected, actual, expected, actual, path, allowUnexpectedKeys, matchers)
      expected is JsonValue.Array && actual is JsonValue.Array ->
        compareLists(expected, actual, expected, actual, path, allowUnexpectedKeys, matchers)
      expected is JsonValue.Object && actual !is JsonValue.Object ||
        expected is JsonValue.Array && actual !is JsonValue.Array ->
        listOf(BodyItemMatchResult(path.joinToString("."),
          listOf(BodyMismatch(expected, actual, "Type mismatch: Expected ${typeOf(expected)} " +
          "${valueOf(expected)} but received ${typeOf(actual)} ${valueOf(actual)}", path.joinToString("."),
          generateJsonDiff(expected, actual)))))
      else -> compareValues(path, expected, actual, matchers)
    }
  }

  private fun compareListContent(
    expectedValues: List<JsonValue>,
    actualValues: List<JsonValue>,
    path: List<String>,
    allowUnexpectedKeys: Boolean,
    matchers: MatchingRules
  ): List<BodyItemMatchResult> {
    val result = mutableListOf<BodyItemMatchResult>()
    for ((index, value) in expectedValues.withIndex()) {
      if (index < actualValues.size) {
        result.addAll(compare(path + index.toString(), value, actualValues[index], allowUnexpectedKeys, matchers))
      } else if (!Matchers.matcherDefined("body", path, matchers)) {
        result.add(BodyItemMatchResult(path.joinToString("."),
          listOf(BodyMismatch(expectedValues, actualValues, "Expected ${valueOf(value)} but was missing",
          path.joinToString("."), generateJsonDiff(jsonArray(expectedValues), jsonArray(actualValues))))))
      }
    }
    return result
  }

  private fun compareLists(
    expectedValues: JsonValue.Array,
    actualValues: JsonValue.Array,
    a: JsonValue,
    b: JsonValue,
    path: List<String>,
    allowUnexpectedKeys: Boolean,
    matchers: MatchingRules
  ): List<BodyItemMatchResult> {
    val expectedList = expectedValues.values
    val actualList = actualValues.values
    return if (Matchers.matcherDefined("body", path, matchers)) {
      logger.debug { "compareLists: Matcher defined for path $path" }
      val result = mutableListOf(BodyItemMatchResult(path.joinToString("."),
        Matchers.domatch(matchers, "body", path, expectedValues, actualValues, BodyMismatchFactory)))
      if (expectedList.isNotEmpty()) {
        result.addAll(compareListContent(expectedList.padTo(actualList.size, expectedList.first()),
          actualList, path, allowUnexpectedKeys, matchers))
      }
      result
    } else {
      if (expectedList.isEmpty() && actualList.isNotEmpty()) {
        listOf(BodyItemMatchResult(path.joinToString("."),
          listOf(BodyMismatch(a, b, "Expected an empty List but received ${valueOf(actualValues)}",
          path.joinToString("."), generateJsonDiff(expectedValues, actualValues)))))
      } else {
        val result = compareListContent(expectedList, actualList, path, allowUnexpectedKeys, matchers).toMutableList()
        if (expectedList.size != actualList.size) {
          result.add(BodyItemMatchResult(path.joinToString("."), listOf(BodyMismatch(a, b,
            "Expected a List with ${expectedList.size} elements but received ${actualList.size} elements",
            path.joinToString("."), generateJsonDiff(expectedValues, actualValues)))))
        }
        result
      }
    }
  }

  private fun compareMaps(
    expectedValues: JsonValue.Object,
    actualValues: JsonValue.Object,
    a: JsonValue,
    b: JsonValue,
    path: List<String>,
    allowUnexpectedKeys: Boolean,
    matchers: MatchingRules
  ): List<BodyItemMatchResult> {
    return if (expectedValues.isEmpty() && actualValues.isNotEmpty() && !allowUnexpectedKeys) {
      listOf(BodyItemMatchResult(path.joinToString("."),
        listOf(BodyMismatch(a, b, "Expected an empty Map but received ${valueOf(actualValues)}",
        path.joinToString("."), generateJsonDiff(expectedValues, actualValues)))))
    } else {
      val result = mutableListOf<BodyItemMatchResult>()
      if (allowUnexpectedKeys && expectedValues.size > actualValues.size) {
        result.add(BodyItemMatchResult(path.joinToString("."), listOf(BodyMismatch(a, b,
          "Expected a Map with at least ${expectedValues.size} elements but received " +
            "${actualValues.size} elements", path.joinToString("."),
          generateJsonDiff(expectedValues, actualValues)))))
      } else if (!allowUnexpectedKeys && expectedValues.size != actualValues.size) {
        result.add(BodyItemMatchResult(path.joinToString("."), listOf(BodyMismatch(a, b,
          "Expected a Map with ${expectedValues.size} elements but received ${actualValues.size} elements",
          path.joinToString("."), generateJsonDiff(expectedValues, actualValues)))))
      }
      if (Matchers.wildcardMatchingEnabled() && Matchers.wildcardMatcherDefined(path + "any", "body", matchers)) {
        actualValues.entries.forEach { (key, value) ->
          if (expectedValues.has(key)) {
            result.addAll(compare(path + key, expectedValues[key], value, allowUnexpectedKeys, matchers))
          } else if (!allowUnexpectedKeys) {
            result.addAll(compare(path + key, expectedValues.entries.values.firstOrNull()
              ?: JsonValue.Null, value, allowUnexpectedKeys, matchers))
          }
        }
      } else {
        expectedValues.entries.forEach { (key, value) ->
          if (actualValues.has(key)) {
            result.addAll(compare(path + key, value, actualValues[key]!!, allowUnexpectedKeys,
              matchers))
          } else {
            result.add(BodyItemMatchResult(path.joinToString("."),
              listOf(BodyMismatch(a, b, "Expected $key=${valueOf(value)} but was missing",
              path.joinToString("."), generateJsonDiff(expectedValues, actualValues)))))
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
    matchers: MatchingRules
  ): List<BodyItemMatchResult> {
    return if (Matchers.matcherDefined("body", path, matchers)) {
      logger.debug { "compareValues: Matcher defined for path $path" }
      listOf(BodyItemMatchResult(path.joinToString("."),
        Matchers.domatch(matchers, "body", path, expected, actual, BodyMismatchFactory)))
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
