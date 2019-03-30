package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.matchingrules.MatchingRules
import mu.KLogging

object QueryMatcher : KLogging() {

  fun compare(
    parameter: String,
    path: List<String>,
    expected: String,
    actual: String,
    matchers: MatchingRules
  ): List<QueryMismatch> {
    return if (Matchers.matcherDefined("query", listOf(parameter), matchers)) {
      logger.debug { "compareQueryParameterValues: Matcher defined for query parameter '$parameter'" }
      Matchers.domatch(matchers, "query", listOf(parameter), expected, actual, QueryMismatchFactory)
    } else {
      logger.debug { "compareQueryParameterValues: No matcher defined for query parameter '$parameter', using equality" }
      if (expected == actual) {
        emptyList()
      } else {
        listOf(QueryMismatch(parameter, expected, actual, "Expected '$expected' but received '$actual' " +
          "for query parameter '$parameter'", parameter))
      }
    }
  }

  fun compareQueryParameterValues(
    parameter: String,
    expected: List<String>,
    actual: List<String>,
    path: List<String>,
    matchers: MatchingRules
  ): List<QueryMismatch> {
    return expected.mapIndexed { index, value -> index to value }
      .flatMap { (index, value) ->
        when {
          index < actual.size -> compare(parameter, path + index.toString(), value, actual[index], matchers)
          !Matchers.matcherDefined("query", path, matchers) ->
            listOf(QueryMismatch(parameter, expected.toString(), actual.toString(),
              "Expected query parameter '$parameter' with value '$value' but was missing",
              path.joinToString(".")))
          else -> emptyList()
        }
      }
  }

  fun compareQuery(
    parameter: String,
    expected: List<String>,
    actual: List<String>,
    matchers: MatchingRules
  ): List<QueryMismatch> {
    val path = listOf(parameter)
    return if (Matchers.matcherDefined("query", path, matchers)) {
      logger.debug { "compareQuery: Matcher defined for query parameter '$parameter'" }
      Matchers.domatch(matchers, "query", path, expected, actual, QueryMismatchFactory) +
        compareQueryParameterValues(parameter, expected, actual, path, matchers)
    } else {
      if (expected.isEmpty() && actual.isNotEmpty()) {
        listOf(QueryMismatch(parameter, expected.toString(), actual.toString(),
          "Expected an empty parameter List for '$parameter' but received $actual",
          path.joinToString(".")))
      } else {
        val mismatches = mutableListOf<QueryMismatch>()
        if (expected.size != actual.size) {
          mismatches.add(QueryMismatch(parameter, expected.toString(), actual.toString(),
            "Expected query parameter '$parameter' with ${expected.size} values but received ${actual.size} values",
            path.joinToString(".")))
        }
        mismatches + compareQueryParameterValues(parameter, expected, actual, path, matchers)
      }
    }
  }
}
