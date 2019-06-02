package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.matchers.util.padTo
import au.com.dius.pact.core.model.HttpPart
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.isEmpty
import au.com.dius.pact.core.model.isMissing
import au.com.dius.pact.core.model.isNull
import au.com.dius.pact.core.model.isPresent
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.valueAsString
import com.github.salomonbrys.kotson.contains
import com.github.salomonbrys.kotson.forEach
import com.github.salomonbrys.kotson.isEmpty
import com.github.salomonbrys.kotson.isNotEmpty
import com.github.salomonbrys.kotson.toJsonArray
import com.github.salomonbrys.kotson.toMap
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import mu.KLogging

object JsonBodyMatcher : BodyMatcher, KLogging() {

  override fun matchBody(
    expected: OptionalBody,
    actual: OptionalBody,
    allowUnexpectedKeys: Boolean,
    matchingRules: MatchingRules
  ): List<BodyMismatch> {
    return when {
      expected.isMissing() -> emptyList()
      expected.isEmpty() && actual.isEmpty() -> emptyList()
      !expected.isEmpty() && actual.isEmpty() ->
        listOf(BodyMismatch(null, actual.valueAsString(), "Expected empty body but received '${actual.value}'"))
      expected.isNull() && actual.isPresent() ->
        listOf(BodyMismatch(null, actual.valueAsString(), "Expected null body but received '${actual.value}'"))
      expected.isNull() -> emptyList()
      actual.isMissing() ->
        listOf(BodyMismatch(expected.valueAsString(), null, "Expected body '${expected.value}' but was missing"))
      else -> {
        val parser = JsonParser()
        compare(listOf("$"), parser.parse(expected.valueAsString()),
          parser.parse(actual.valueAsString()), allowUnexpectedKeys, matchingRules)
      }
    }
  }

  fun valueOf(value: Any?) = when (value) {
    is String -> "'$value'"
    null -> "null"
    else -> value.toString()
  }

  fun typeOf(value: Any?) = when (value) {
    is Map<*, *> -> "Map"
    is JsonObject -> "Map"
    is List<*> -> "List"
    is JsonArray -> "List"
    is JsonNull -> "Null"
    is JsonPrimitive -> "Primitive"
    null -> "Null"
    else -> value.javaClass.simpleName
  }

  fun compare(
    path: List<String>,
    expected: JsonElement,
    actual: JsonElement,
    allowUnexpectedKeys: Boolean,
    matchers: MatchingRules
  ): List<BodyMismatch> {
    return when {
      expected is JsonObject && actual is JsonObject ->
        compareMaps(expected, actual, expected, actual, path, allowUnexpectedKeys, matchers)
      expected is JsonArray && actual is JsonArray ->
        compareLists(expected, actual, expected, actual, path, allowUnexpectedKeys, matchers)
      expected is JsonObject && actual !is JsonObject || expected is JsonArray && actual !is JsonArray ->
        listOf(BodyMismatch(expected, actual, "Type mismatch: Expected ${typeOf(expected)} " +
          "${valueOf(expected)} but received ${typeOf(actual)} ${valueOf(actual)}", path.joinToString("."),
          generateJsonDiff(expected, actual)))
      else -> compareValues(path, expected, actual, matchers)
    }
  }

  fun compareListContent(
    expectedValues: List<JsonElement>,
    actualValues: List<JsonElement>,
    path: List<String>,
    allowUnexpectedKeys: Boolean,
    matchers: MatchingRules
  ): List<BodyMismatch> {
    val result = mutableListOf<BodyMismatch>()
    for ((index, value) in expectedValues.withIndex()) {
      if (index < actualValues.size) {
        result.addAll(compare(path + index.toString(), value, actualValues[index], allowUnexpectedKeys, matchers))
      } else if (!Matchers.matcherDefined("body", path, matchers)) {
        result.add(BodyMismatch(expectedValues, actualValues, "Expected ${valueOf(value)} but was missing",
          path.joinToString("."), generateJsonDiff(expectedValues.toJsonArray(), actualValues.toJsonArray())))
      }
    }
    return result
  }

  fun compareLists(
    expectedValues: JsonArray,
    actualValues: JsonArray,
    a: JsonElement,
    b: JsonElement,
    path: List<String>,
    allowUnexpectedKeys: Boolean,
    matchers: MatchingRules
  ): List<BodyMismatch> {
    val expectedList = expectedValues.toList()
    val actualList = actualValues.toList()
    return if (Matchers.matcherDefined("body", path, matchers)) {
      logger.debug { "compareLists: Matcher defined for path $path" }
      val result = Matchers.domatch(matchers, "body", path, expectedValues, actualValues, BodyMismatchFactory)
        .toMutableList()
      if (expectedList.isNotEmpty()) {
        result.addAll(compareListContent(expectedList.padTo(actualValues.size(), expectedValues.first()),
          actualList, path, allowUnexpectedKeys, matchers))
      }
      result
    } else {
      if (expectedList.isEmpty() && actualList.isNotEmpty()) {
        listOf(BodyMismatch(a, b, "Expected an empty List but received ${valueOf(actualValues)}",
          path.joinToString("."), generateJsonDiff(expectedValues, actualValues)))
      } else {
        val result = compareListContent(expectedList, actualList, path, allowUnexpectedKeys, matchers).toMutableList()
        if (expectedValues.size() != actualValues.size()) {
          result.add(BodyMismatch(a, b,
            "Expected a List with ${expectedValues.size()} elements but received ${actualValues.size()} elements",
            path.joinToString("."), generateJsonDiff(expectedValues, actualValues)))
        }
        result
      }
    }
  }

  fun compareMaps(
    expectedValues: JsonObject,
    actualValues: JsonObject,
    a: JsonElement,
    b: JsonElement,
    path: List<String>,
    allowUnexpectedKeys: Boolean,
    matchers: MatchingRules
  ): List<BodyMismatch> {
    return if (expectedValues.isEmpty() && actualValues.isNotEmpty() && !allowUnexpectedKeys) {
      listOf(BodyMismatch(a, b, "Expected an empty Map but received ${valueOf(actualValues)}",
        path.joinToString("."), generateJsonDiff(expectedValues, actualValues)))
    } else {
      val result = mutableListOf<BodyMismatch>()
      if (allowUnexpectedKeys && expectedValues.size() > actualValues.size()) {
        result.add(BodyMismatch(a, b,
          "Expected a Map with at least ${expectedValues.size()} elements but received " +
            "${actualValues.size()} elements", path.joinToString("."),
          generateJsonDiff(expectedValues, actualValues)))
      } else if (!allowUnexpectedKeys && expectedValues.size() != actualValues.size()) {
        result.add(BodyMismatch(a, b,
          "Expected a Map with ${expectedValues.size()} elements but received ${actualValues.size()} elements",
          path.joinToString("."), generateJsonDiff(expectedValues, actualValues)))
      }
      if (Matchers.wildcardMatchingEnabled() && Matchers.wildcardMatcherDefined(path + "any", "body", matchers)) {
        actualValues.forEach { key, value ->
          if (expectedValues.contains(key)) {
            result.addAll(compare(path + key, expectedValues[key]!!, value, allowUnexpectedKeys, matchers))
          } else if (!allowUnexpectedKeys) {
            result.addAll(compare(path + key, expectedValues.toMap().values.firstOrNull()
              ?: JsonNull.INSTANCE, value, allowUnexpectedKeys, matchers))
          }
        }
      } else {
        expectedValues.forEach { key, value ->
          if (actualValues.contains(key)) {
            result.addAll(compare(path + key, value, actualValues[key]!!, allowUnexpectedKeys,
              matchers))
          } else {
            result.add(BodyMismatch(a, b, "Expected $key=${valueOf(value)} but was missing",
              path.joinToString("."), generateJsonDiff(expectedValues, actualValues)))
          }
        }
      }
      result
    }
  }

  fun compareValues(
    path: List<String>,
    expected: JsonElement,
    actual: JsonElement,
    matchers: MatchingRules
  ): List<BodyMismatch> {
    return if (Matchers.matcherDefined("body", path, matchers)) {
      logger.debug { "compareValues: Matcher defined for path $path" }
      Matchers.domatch(matchers, "body", path, expected, actual, BodyMismatchFactory)
    } else {
      logger.debug { "compareValues: No matcher defined for path $path, using equality" }
      if (expected == actual) {
        emptyList()
      } else {
        listOf(BodyMismatch(expected, actual, "Expected ${valueOf(expected)} but received ${valueOf(actual)}",
          path.joinToString(".")))
      }
    }
  }
}
