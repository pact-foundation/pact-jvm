package au.com.dius.pact.matchers

import au.com.dius.pact.model.{QueryMismatchFactory, QueryMismatch}
import au.com.dius.pact.com.typesafe.scalalogging.StrictLogging

object QueryMatcher extends StrictLogging {

  def compare(parameter: String, path: Seq[String], expected: String, actual: String, matchers: Option[Map[String, Map[String, Any]]]) = {
    if (Matchers.matcherDefined(path, matchers)) {
      logger.debug("compareQueryParameterValues: Matcher defined for path " + path)
      Matchers.domatch[QueryMismatch](matchers, path, expected, actual, QueryMismatchFactory)
    } else {
      logger.debug("compareQueryParameterValues: No matcher defined for path " + path + ", using equality")
      if (expected == actual) {
        Seq[QueryMismatch]()
      } else {
        Seq(QueryMismatch(parameter, expected, actual, Some(s"Expected '$expected' but received '$actual' for query parameter '$parameter'"),
          path.mkString(".")))
      }
    }
  }

  def compareQueryParameterValues(parameter: String, expected: List[String], actual: List[String],
                                  path: Seq[String], matchers: Option[Map[String, Map[String, Any]]]) = {
    var result = Seq[QueryMismatch]()
    for ((value, index) <- expected.view.zipWithIndex) {
      if (index < actual.size) {
        result = result ++: compare(parameter, path :+ index.toString, value, actual(index), matchers)
      } else if (!Matchers.matcherDefined(path, matchers)) {
        result = result :+ QueryMismatch(parameter, expected.toString(), actual.toString(),
          Some(s"Expected query parameter $parameter value $value but was missing"),
          path.mkString("."))
      }
    }
    result
  }

  def compareQuery(parameter: String, expected: List[String], actual: List[String], matchers: Option[Map[String, Map[String, Any]]]) = {
    var result = Seq[QueryMismatch]()
    val path = Seq("$", "query", parameter)
    if (Matchers.matcherDefined(path, matchers)) {
      logger.debug("compareQuery: Matcher defined for path " + path)
      result = Matchers.domatch[QueryMismatch](matchers, path, expected, actual, QueryMismatchFactory) ++
        compareQueryParameterValues(parameter, expected, actual, path, matchers)
    } else {
      if (expected.isEmpty && actual.nonEmpty) {
        result = Seq(QueryMismatch(parameter, expected.toString(), actual.toString(), Some(s"Expected an empty parameter List for $parameter but received $actual"),
          path.mkString(".")))
      } else {
        if (expected.size != actual.size) {
          result = result :+ QueryMismatch(parameter, expected.toString(), actual.toString(),
            Some(s"Expected query parameter $parameter with ${expected.size} values but received ${actual.size} values"),
            path.mkString("."))
        }
        result = result ++ compareQueryParameterValues(parameter, expected, actual, path, matchers)
      }
    }
    result
  }
}
