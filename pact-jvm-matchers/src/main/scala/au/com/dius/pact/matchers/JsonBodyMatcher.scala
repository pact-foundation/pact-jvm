package au.com.dius.pact.matchers

import au.com.dius.pact.matchers.util.JsonUtils
import au.com.dius.pact.model._
import au.com.dius.pact.model.matchingrules.MatchingRules
import com.typesafe.scalalogging.StrictLogging

class JsonBodyMatcher extends BodyMatcher with StrictLogging {

  def matchBody(expected: HttpPart, actual: HttpPart, allowUnexpectedKeys: Boolean): List[BodyMismatch] = {
    (expected.getBody.getState, actual.getBody.getState) match {
      case (OptionalBody.State.MISSING, _) => List()
      case (OptionalBody.State.NULL, OptionalBody.State.PRESENT) => List(BodyMismatch(None, actual.getBody.getValue,
        Some(s"Expected empty body but received '${actual.getBody.getValue}'")))
      case (OptionalBody.State.NULL, _) => List()
      case (_, OptionalBody.State.MISSING) => List(BodyMismatch(expected.getBody.getValue, None,
        Some(s"Expected body '${expected.getBody.getValue}' but was missing")))
      case (_, _) => compare(Seq("$"), JsonUtils.parseJsonString(expected.getBody.getValue),
        JsonUtils.parseJsonString(actual.getBody.getValue), allowUnexpectedKeys, expected.getMatchingRules)
    }
  }

  def valueOf(value: Any) = {
    value match {
      case s: String => s"'$value'"
      case null => "null"
      case _ => value.toString
    }
  }

  def typeOf(value: Any) = {
    if (value == null) {
      "Null"
    } else value match {
      case _: Map[Any, Any] =>
        "Map"
      case _: List[Any] =>
        "List"
      case _ =>
        value.getClass.getSimpleName
    }
  }

  def compare(path: Seq[String], expected: Any, actual: Any, allowUnexpectedKeys: Boolean,
              matchers: MatchingRules): List[BodyMismatch] = {
    (expected, actual) match {
      case (a: Map[String, Any], b: Map[String, Any]) => compareMaps(a, b, a, b, path, allowUnexpectedKeys, matchers)
      case (a: List[Any], b: List[Any]) => compareLists(a, b, a, b, path, allowUnexpectedKeys, matchers)
      case (_, _) =>
        if ((expected.isInstanceOf[Map[String, Any]] && !actual.isInstanceOf[Map[String, Any]]) ||
          (expected.isInstanceOf[List[Any]] && !actual.isInstanceOf[List[Any]])) {
          List(BodyMismatch(expected, actual,
            Some(s"Type mismatch: Expected ${typeOf(expected)} ${valueOf(expected)} but received ${typeOf(actual)} ${valueOf(actual)}"),
            path.mkString("."), generateObjectDiff(expected, actual)))
        } else {
          compareValues(path, expected, actual, matchers)
        }
    }
  }

  private def generateObjectDiff(expected: Any, actual: Any) = {
    Some(DiffUtils.generateObjectDiff(JsonUtils.scalaObjectGraphToJavaObjectGraph(expected),
      JsonUtils.scalaObjectGraphToJavaObjectGraph(actual)))
  }

  def compareListContent(expectedValues: List[Any], actualValues: List[Any], path: Seq[String],
                         allowUnexpectedKeys: Boolean, matchers: MatchingRules) = {
    var result = List[BodyMismatch]()
    for ((value, index) <- expectedValues.view.zipWithIndex) {
      if (index < actualValues.size) {
        result = result ++: compare(path :+ index.toString, value, actualValues(index), allowUnexpectedKeys, matchers)
      } else if (!Matchers.matcherDefined("body", path, matchers)) {
        result = result :+ BodyMismatch(expectedValues, actualValues, Some(s"Expected ${valueOf(value)} but was missing"),
          path.mkString("."), generateObjectDiff(expectedValues, actualValues))
      }
    }
    result
  }

  def compareLists(expectedValues: List[Any], actualValues: List[Any], a: Any, b: Any, path: Seq[String],
                   allowUnexpectedKeys: Boolean, matchers: MatchingRules): List[BodyMismatch] = {
    if (Matchers.matcherDefined("body", path, matchers)) {
      logger.debug("compareLists: Matcher defined for path " + path)
      var result = Matchers.domatch[BodyMismatch](matchers, "body", path, expectedValues, actualValues, BodyMismatchFactory)
      if (expectedValues.nonEmpty) {
        result = result ++ compareListContent(expectedValues.padTo(actualValues.length, expectedValues.head),
          actualValues, path, allowUnexpectedKeys, matchers)
      }
      result
    } else {
      if (expectedValues.isEmpty && actualValues.nonEmpty) {
        List(BodyMismatch(a, b, Some(s"Expected an empty List but received ${valueOf(actualValues)}"),
          path.mkString("."), generateObjectDiff(expectedValues, actualValues)))
      } else {
        var result = compareListContent(expectedValues, actualValues, path, allowUnexpectedKeys, matchers)
        if (expectedValues.size != actualValues.size) {
          result = result :+ BodyMismatch(a, b,
            Some(s"Expected a List with ${expectedValues.size} elements but received ${actualValues.size} elements"),
            path.mkString("."), generateObjectDiff(expectedValues, actualValues))
        }
        result
      }
    }
  }

  def compareMaps(expectedValues: Map[String, Any], actualValues: Map[String, Any], a: Any, b: Any, path: Seq[String],
                  allowUnexpectedKeys: Boolean, matchers: MatchingRules): List[BodyMismatch] = {
    if (expectedValues.isEmpty && actualValues.nonEmpty) {
      List(BodyMismatch(a, b, Some(s"Expected an empty Map but received ${valueOf(actualValues)}"), path.mkString("."),
        generateObjectDiff(expectedValues, actualValues)))
    } else {
      var result = List[BodyMismatch]()
      if (allowUnexpectedKeys && expectedValues.size > actualValues.size) {
        result = result :+ BodyMismatch(a, b,
          Some(s"Expected a Map with at least ${expectedValues.size} elements but received ${actualValues.size} elements"),
          path.mkString("."), generateObjectDiff(expectedValues, actualValues))
      } else if (!allowUnexpectedKeys && expectedValues.size != actualValues.size) {
        result = result :+ BodyMismatch(a, b,
          Some(s"Expected a Map with ${expectedValues.size} elements but received ${actualValues.size} elements"),
          path.mkString("."), generateObjectDiff(expectedValues, actualValues))
      }
      if (Matchers.wildcardMatcherDefined(path :+ "any", "body", matchers)) {
        actualValues.foreach(entry => {
          if (expectedValues.contains(entry._1)) {
            result = result ++: compare(path :+ entry._1, expectedValues.apply(entry._1), entry._2, allowUnexpectedKeys, matchers)
          } else if (!allowUnexpectedKeys) {
            result = result ++: compare(path :+ entry._1, expectedValues.values.head, entry._2, allowUnexpectedKeys, matchers)
          }
        })
      } else {
        expectedValues.foreach(entry => {
          if (actualValues.contains(entry._1)) {
            result = result ++: compare(path :+ entry._1, entry._2, actualValues(entry._1), allowUnexpectedKeys, matchers)
          } else {
            result = result :+ BodyMismatch(a, b, Some(s"Expected ${entry._1}=${valueOf(entry._2)} but was missing"),
              path.mkString("."), generateObjectDiff(expectedValues, actualValues))
          }
        })
      }
      result
    }
  }

  def compareValues(path: Seq[String], expected: Any, actual: Any, matchers: MatchingRules): List[BodyMismatch] = {
    if (Matchers.matcherDefined("body", path, matchers)) {
      logger.debug("compareValues: Matcher defined for path " + path)
      Matchers.domatch[BodyMismatch](matchers, "body", path, expected, actual, BodyMismatchFactory)
    } else {
      logger.debug("compareValues: No matcher defined for path " + path + ", using equality")
      if (expected == actual) {
        List[BodyMismatch]()
      } else {
        List(BodyMismatch(expected, actual, Some(s"Expected ${valueOf(expected)} but received ${valueOf(actual)}"),
          path.mkString("."), None))
      }
    }
  }
}
