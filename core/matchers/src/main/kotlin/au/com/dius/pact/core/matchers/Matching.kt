package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.HttpPart
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.isEmpty
import au.com.dius.pact.core.model.isMissing
import au.com.dius.pact.core.model.isNull
import au.com.dius.pact.core.model.isPresent
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.unwrap
import mu.KLogging

object Matching : KLogging() {

  val pathFilter = Regex("http[s]*://([^/]*)")

  fun matchHeaders(
    expected: Map<String, String>,
    actual: Map<String, String>,
    matchers: MatchingRules
  ): List<HeaderMismatch> {
    return expected.asSequence().fold(listOf<HeaderMismatch?>()) { list, entry ->
      val actualKey = actual.keys.find { it.equals(entry.key, ignoreCase = true) }
      if (actualKey != null) {
        list + HeaderMatcher.compareHeader(entry.key, entry.value, actual[actualKey]!!, matchers)
      } else {
        list + HeaderMismatch(entry.key, entry.value, "", "Expected a header '${entry.key}' but was missing")
      }
    }.filterNotNull()
  }

  fun matchRequestHeaders(expected: Request, actual: Request) =
    matchHeaders(expected.headersWithoutCookie() ?: emptyMap(), actual.headersWithoutCookie() ?: emptyMap(),
      expected.matchingRules ?: MatchingRulesImpl())

  fun matchHeaders(expected: HttpPart, actual: HttpPart) =
    matchHeaders(expected.headers ?: mapOf(), actual.headers ?: mapOf(),
      expected.matchingRules ?: MatchingRulesImpl())

  fun matchCookie(expected: List<String>, actual: List<String>) =
    if (expected.all { actual.contains(it) }) null
    else CookieMismatch(expected, actual)

  fun matchMethod(expected: String, actual: String) =
    if (expected.equals(actual, ignoreCase = true)) null
    else MethodMismatch(expected, actual)

  fun matchBody(expected: HttpPart, actual: HttpPart, allowUnexpectedKeys: Boolean): List<Mismatch> {
    return if (expected.mimeType() == actual.mimeType()) {
      val matcher = MatchingConfig.lookupBodyMatcher(actual.mimeType())
      if (matcher != null) {
        logger.debug { "Found a matcher for ${actual.mimeType()} -> $matcher" }
        matcher.matchBody(expected, actual, allowUnexpectedKeys)
      } else {
        logger.debug { "No matcher for ${actual.mimeType()}, using equality" }
        when {
          expected.body.isMissing() -> emptyList()
          expected.body.isNull() && actual.body.isPresent() -> listOf(BodyMismatch(null, actual.body.unwrap(),
            "Expected an empty body but received '${actual.body.unwrap()}'"))
          expected.body.isNull() -> emptyList()
          actual.body.isMissing() -> listOf(BodyMismatch(expected.body.unwrap(), null,
            "Expected body '${expected.body.unwrap()}' but was missing"))
          expected.body.unwrap() == actual.body.unwrap() -> emptyList()
          else -> listOf(BodyMismatch(expected.body.unwrap(), actual.body.unwrap()))
        }
      }
    } else {
      if (expected.body.isMissing() || expected.body.isNull() || expected.body.isEmpty()) emptyList()
      else listOf(BodyTypeMismatch(expected.mimeType(), actual.mimeType()))
    }
  }

  fun matchPath(expected: Request, actual: Request): PathMismatch? {
    val replacedActual = actual.path.replaceFirst(pathFilter, "")
    val matchers = expected.matchingRules ?: MatchingRulesImpl()
    return if (Matchers.matcherDefined("path", emptyList(), matchers)) {
      val mismatch = Matchers.domatch(matchers, "path", emptyList(), expected.path,
        replacedActual, PathMismatchFactory)
      mismatch.firstOrNull()
    } else if (expected.path == replacedActual || replacedActual.matches(Regex(expected.path))) null
    else PathMismatch(expected.path, replacedActual)
  }

  fun matchStatus(expected: Int, actual: Int) = if (expected == actual) null else StatusMismatch(expected, actual)

  fun matchQuery(expected: Request, actual: Request): List<QueryMismatch> {
    return (expected.query ?: emptyMap()).entries.fold(emptyList<QueryMismatch>()) { acc, entry ->
      val value = actual.query[entry.key]
      when (value) {
        null -> acc + QueryMismatch(entry.key, entry.value.joinToString(","), "",
          "Expected query parameter '${entry.key}' but was missing",
          listOf("$", "query", entry.key).joinToString("."))
        else -> acc + QueryMatcher.compareQuery(entry.key, entry.value, value,
          expected.matchingRules ?: MatchingRulesImpl())
      }
    } + (actual.query ?: emptyMap()).entries.fold(emptyList<QueryMismatch>()) { acc, entry ->
      when (expected.query[entry.key]) {
        null -> acc + QueryMismatch(entry.key, "", entry.value.joinToString(","),
          "Unexpected query parameter '${entry.key}' received",
          listOf("$", "query", entry.key).joinToString("."))
        else -> acc
      }
    }
  }
}
