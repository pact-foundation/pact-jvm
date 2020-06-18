package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.HttpPart
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import mu.KLogging

object Matching : KLogging() {
  private val lowerCaseComparator = Comparator<String> { a, b -> a.toLowerCase().compareTo(b.toLowerCase()) }

  val pathFilter = Regex("http[s]*://([^/]*)")

  @JvmStatic
  fun matchRequestHeaders(expected: Request, actual: Request) =
    matchHeaders(expected.headersWithoutCookie(), actual.headersWithoutCookie(), expected.matchingRules)

  @JvmStatic
  fun matchHeaders(expected: HttpPart, actual: HttpPart): List<HeaderMismatch> =
    matchHeaders(expected.headers.orEmpty(), actual.headers.orEmpty(), expected.matchingRules)

  @JvmStatic
  fun matchHeaders(
    expected: Map<String, List<String>>,
    actual: Map<String, List<String>>,
    matchers: MatchingRules?
  ): List<HeaderMismatch> = compareHeaders(expected.toSortedMap(lowerCaseComparator),
    actual.toSortedMap(lowerCaseComparator), matchers)

  fun compareHeaders(
    e: Map<String, List<String>>,
    a: Map<String, List<String>>,
    matchers: MatchingRules?
  ): List<HeaderMismatch> {
    return e.entries.fold(listOf()) { list, values ->
      if (a.containsKey(values.key)) {
        val actual = a[values.key].orEmpty()
        list + values.value.mapIndexed { index, headerValue ->
          HeaderMatcher.compareHeader(values.key, headerValue, actual.getOrElse(index) { "" },
            matchers ?: MatchingRulesImpl())
        }.filterNotNull()
      } else {
        list + HeaderMismatch(values.key, values.value.joinToString(separator = ", "), "",
          "Expected a header '${values.key}' but was missing")
      }
    }
  }

  fun matchCookie(expected: List<String>, actual: List<String>) =
    if (expected.all { actual.contains(it) }) null
    else CookieMismatch(expected, actual)

  fun matchMethod(expected: String, actual: String) =
    if (expected.equals(actual, ignoreCase = true)) null
    else MethodMismatch(expected, actual)

  fun matchBody(expected: HttpPart, actual: HttpPart, allowUnexpectedKeys: Boolean): List<Mismatch> {
    val expectedContentType = expected.determineContentType()
    val actualContentType = actual.determineContentType()
    return if (expectedContentType.getBaseType() == actualContentType.getBaseType()) {
      val matcher = MatchingConfig.lookupBodyMatcher(actualContentType.getBaseType())
      if (matcher != null) {
        logger.debug { "Found a matcher for $actualContentType -> $matcher" }
        matcher.matchBody(expected.body, actual.body, allowUnexpectedKeys, expected.matchingRules)
      } else {
        logger.debug { "No matcher for $actualContentType, using equality" }
        when {
          expected.body.isMissing() -> emptyList()
          expected.body.isNull() && actual.body.isPresent() -> listOf(BodyMismatch(null, actual.body.unwrap(),
            "Expected an empty body but received '${actual.body.unwrap()}'"))
          expected.body.isNull() -> emptyList()
          actual.body.isMissing() -> listOf(BodyMismatch(expected.body.unwrap(), null,
            "Expected body '${expected.body.unwrap()}' but was missing"))
          else -> matchBodyContents(expected, actual)
        }
      }
    } else {
      if (expected.body.isMissing() || expected.body.isNull() || expected.body.isEmpty()) emptyList()
      else listOf(BodyTypeMismatch(expectedContentType.getBaseType(), actualContentType.getBaseType()))
    }
  }

  fun matchBodyContents(expected: HttpPart, actual: HttpPart): List<BodyMismatch> {
    val matcher = expected.matchingRules.rulesForCategory("body").matchingRules["$"]
    return when {
      matcher != null -> domatch(matcher, listOf("$"), expected.body.unwrap(), actual.body.unwrap(), BodyMismatchFactory)
      expected.body.unwrap().contentEquals(actual.body.unwrap()) -> emptyList()
      else -> listOf(BodyMismatch(expected.body.unwrap(), actual.body.unwrap(),
        "Actual body '${actual.body.valueAsString()}' is not equal to the expected body " +
          "'${expected.body.valueAsString()}'"))
    }
  }

  fun matchPath(expected: Request, actual: Request): PathMismatch? {
    val replacedActual = actual.path.replaceFirst(pathFilter, "")
    val matchers = expected.matchingRules
    return if (Matchers.matcherDefined("path", emptyList(), matchers)) {
      val mismatch = Matchers.domatch(matchers, "path", emptyList(), expected.path,
        replacedActual, PathMismatchFactory)
      mismatch.firstOrNull()
    } else if (expected.path == replacedActual || replacedActual.matches(Regex(expected.path))) null
    else PathMismatch(expected.path, replacedActual)
  }

  fun matchStatus(expected: Int, actual: Int) = if (expected == actual) null else StatusMismatch(expected, actual)

  fun matchQuery(expected: Request, actual: Request): List<QueryMismatch> {
    return expected.query.entries.fold(emptyList<QueryMismatch>()) { acc, entry ->
      when (val value = actual.query[entry.key]) {
        null -> acc + QueryMismatch(entry.key, entry.value.joinToString(","), "",
          "Expected query parameter '${entry.key}' but was missing",
          listOf("$", "query", entry.key).joinToString("."))
        else -> acc + QueryMatcher.compareQuery(entry.key, entry.value, value, expected.matchingRules)
      }
    } + actual.query.entries.fold(emptyList<QueryMismatch>()) { acc, entry ->
      when (expected.query[entry.key]) {
        null -> acc + QueryMismatch(entry.key, "", entry.value.joinToString(","),
          "Unexpected query parameter '${entry.key}' received",
          listOf("$", "query", entry.key).joinToString("."))
        else -> acc
      }
    }
  }

  @JvmStatic
  fun compareMessageMetadata(
    e: Map<String, Any?>,
    a: Map<String, Any?>,
    matchers: MatchingRules?
  ): List<MetadataMismatch> {
    return e.entries.fold(listOf()) { list, value ->
      if (a.containsKey(value.key)) {
        val actual = a[value.key]
        val compare = MetadataMatcher.compare(value.key, value.value, actual, matchers ?: MatchingRulesImpl())
        if (compare != null) list + compare else list
      } else if (value.key.toLowerCase() != "contenttype" && value.key.toLowerCase() != "content-type") {
        list + MetadataMismatch(value.key, value.value, null,
          "Expected metadata '${value.key}' but was missing")
      } else {
        list
      }
    }
  }
}
