package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.matchingrules.MatchingRules
import mu.KLogging

object HeaderMatcher : KLogging() {

  fun matchContentType(expected: String, actual: String): HeaderMismatch? {
    logger.debug { "Comparing content type header: '$actual' to '$expected'" }

    val expectedValues = expected.split(';').map { it.trim() }
    val actualValues = actual.split(';').map { it.trim() }
    val expectedContentType = expectedValues.first()
    val actualContentType = actualValues.first()
    val expectedParameters = parseParameters(expectedValues.drop(1))
    val actualParameters = parseParameters(actualValues.drop(1))
    val headerMismatch = HeaderMismatch("Content-Type", expected, actual,
      "Expected header 'Content-Type' to have value '$expected' but was '$actual'")

    return if (expectedContentType.equals(actualContentType, ignoreCase = true)) {
      expectedParameters.map { entry ->
        if (actualParameters.contains(entry.key)) {
          if (entry.value.equals(actualParameters[entry.key], ignoreCase = true)) null
          else headerMismatch
        } else headerMismatch
      }.filterNotNull().firstOrNull()
    } else {
      headerMismatch
    }
  }

  @JvmStatic
  fun parseParameters(values: List<String>): Map<String, String> {
    return values.map { it.split('=').map { it.trim() } }.associate { it.first() to it.component2() }
  }

  fun stripWhiteSpaceAfterCommas(str: String): String = Regex(",\\s*").replace(str, ",")

  /**
   * Compares the expected header value to the actual, delegating to any matching rules if present
   */
  @JvmStatic
  fun compareHeader(headerKey: String, expected: String, actual: String, matchers: MatchingRules): HeaderMismatch? {
    logger.debug { "Comparing header '$headerKey': '$actual' to '$expected'" }

    val comparator = Comparator<String> { a, b -> a.compareTo(b, ignoreCase = true) }
    return when {
      Matchers.matcherDefined("header", listOf(headerKey), matchers, comparator) -> {
        val matchResult = Matchers.domatch(matchers, "header", listOf(headerKey), expected, actual,
          HeaderMismatchFactory, comparator)
        return matchResult.fold(null as HeaderMismatch?) { acc, item -> acc?.merge(item) ?: item }
      }
      headerKey.equals("Content-Type", ignoreCase = true) -> matchContentType(expected, actual)
      stripWhiteSpaceAfterCommas(expected) == stripWhiteSpaceAfterCommas(actual) -> null
      else -> HeaderMismatch(headerKey, expected, actual, "Expected header '$headerKey' to have value " +
        "'$expected' but was '$actual'")
    }
  }
}
