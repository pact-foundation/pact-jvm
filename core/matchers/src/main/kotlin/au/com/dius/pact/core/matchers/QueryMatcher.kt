package au.com.dius.pact.core.matchers

import mu.KLogging
import org.atteo.evo.inflector.English

object QueryMatcher : KLogging() {

  private fun compare(
    parameter: String,
    expected: String,
    actual: String,
    context: MatchingContext
  ): List<QueryMismatch> {
    return if (context.matcherDefined(listOf(parameter))) {
      logger.debug { "compareQueryParameterValues: Matcher defined for query parameter '$parameter'" }
      Matchers.domatch(context, listOf(parameter), expected, actual, QueryMismatchFactory)
    } else {
      logger
        .debug { "compareQueryParameterValues: No matcher defined for query parameter '$parameter', using equality" }
      if (expected == actual) {
        emptyList()
      } else {
        listOf(QueryMismatch(parameter, expected, actual, "Expected '$expected' but received '$actual' " +
          "for query parameter '$parameter'", parameter))
      }
    }
  }

  private fun compareQueryParameterValues(
    parameter: String,
    expected: List<String>,
    actual: List<String>,
    path: List<String>,
    context: MatchingContext
  ): List<QueryMismatch> {
    return expected.mapIndexed { index, value -> index to value }
      .flatMap { (index, value) ->
        when {
          index < actual.size -> compare(parameter, value, actual[index], context)
          !context.matcherDefined(path) ->
            listOf(QueryMismatch(parameter, expected.toString(), actual.toString(),
              "Expected query parameter '$parameter' with value '$value' but was missing",
              path.joinToString(".")))
          else -> emptyList()
        }
      }
  }

  @JvmStatic
  fun compareQuery(
    parameter: String,
    expected: List<String>,
    actual: List<String>,
    context: MatchingContext
  ): List<QueryMismatch> {
    val path = listOf(parameter)
    return if (context.matcherDefined(path)) {
      logger.debug { "compareQuery: Matcher defined for query parameter '$parameter'" }
      Matchers.domatch(context, path, expected, actual, QueryMismatchFactory) +
        compareQueryParameterValues(parameter, expected, actual, path, context)
    } else {
      if (expected.isEmpty() && actual.isNotEmpty()) {
        listOf(QueryMismatch(parameter, expected.toString(), actual.toString(),
          "Expected an empty parameter List for '$parameter' but received $actual",
          path.joinToString(".")))
      } else {
        val mismatches = mutableListOf<QueryMismatch>()
        if (expected.size != actual.size) {
          mismatches.add(QueryMismatch(parameter, expected.toString(), actual.toString(),
            "Expected query parameter '$parameter' with ${expected.size} " +
              "${English.plural("value", expected.size)} but received ${actual.size} " +
              English.plural("value", actual.size),
            path.joinToString(".")))
        }
        mismatches + compareQueryParameterValues(parameter, expected, actual, path, context)
      }
    }
  }
}
