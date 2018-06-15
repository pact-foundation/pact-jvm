package au.com.dius.pact.matchers

import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.model.{QueryMismatch, QueryMismatchFactory}
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._

object QueryMatcher extends StrictLogging {

  def compare(parameter: String, path: Seq[String], expected: String, actual: String, matchers: MatchingRules) = {
    if (Matchers.matcherDefined("query", Seq(parameter).asJava, matchers)) {
      logger.debug("compareQueryParameterValues: Matcher defined for query parameter " + parameter)
      Matchers.domatch[QueryMismatch](matchers, "query", Seq(parameter).asJava, expected, actual, QueryMismatchFactory).asScala.toList
    } else {
      logger.debug("compareQueryParameterValues: No matcher defined for query parameter " + parameter + ", using equality")
      if (expected == actual) {
        Seq[QueryMismatch]()
      } else {
        Seq(QueryMismatch(parameter, expected, actual, Some(s"Expected '$expected' but received '$actual' for query parameter '$parameter'"),
          parameter))
      }
    }
  }

  def compareQueryParameterValues(parameter: String, expected: List[String], actual: List[String],
                                  path: Seq[String], matchers: MatchingRules) = {
    var result = Seq[QueryMismatch]()
    for ((value, index) <- expected.view.zipWithIndex) {
      if (index < actual.size) {
        result = result ++: compare(parameter, path :+ index.toString, value, actual(index), matchers)
      } else if (!Matchers.matcherDefined("query", path.asJava, matchers)) {
        result = result :+ QueryMismatch(parameter, expected.toString(), actual.toString(),
          Some(s"Expected query parameter $parameter value $value but was missing"),
          path.mkString("."))
      }
    }
    result
  }

  def compareQuery(parameter: String, expected: List[String], actual: List[String], matchers: MatchingRules) = {
    var result = Seq[QueryMismatch]()
    val path = Seq(parameter)
    if (Matchers.matcherDefined("query", path.asJava, matchers)) {
      logger.debug("compareQuery: Matcher defined for query parameter " + parameter)
      result = Matchers.domatch[QueryMismatch](matchers, "query", path.asJava, expected, actual, QueryMismatchFactory).asScala.toList ++
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
