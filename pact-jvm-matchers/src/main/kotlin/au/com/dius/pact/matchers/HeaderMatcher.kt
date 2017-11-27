package au.com.dius.pact.matchers

import au.com.dius.pact.model.matchingrules.MatchingRules
import mu.KLogging
import scala.collection.JavaConverters

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

    return if (expectedContentType == actualContentType) {
      expectedParameters.map { entry ->
        if (actualParameters.contains(entry.key)) {
          if (entry.value == actualParameters[entry.key]) null
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

  private fun toScalaSeq(value: String) = JavaConverters.asScalaIterator(listOf(value).iterator()).toSeq()

  @JvmStatic
  fun compareHeader(headerKey: String, expected: String, actual: String, matchers: MatchingRules): HeaderMismatch? {
    logger.debug { "Comparing header '$headerKey': '$actual' to '$expected'" }

    if (Matchers.matcherDefined("header", listOf(headerKey), matchers)) {
      return Matchers.domatch<HeaderMismatch>(matchers, "header", listOf(headerKey), expected, actual,
        HeaderMismatchFactory).firstOrNull()
    } else if (headerKey.equals("Content-Type", ignoreCase = true)) {
      return matchContentType(expected, actual)
    } else if (stripWhiteSpaceAfterCommas(expected) == stripWhiteSpaceAfterCommas(actual)) {
      return null
    } else {
      return HeaderMismatch(headerKey, expected, actual, "Expected header '$headerKey' to have value '$expected' but was '$actual'")
    }
  }
}
