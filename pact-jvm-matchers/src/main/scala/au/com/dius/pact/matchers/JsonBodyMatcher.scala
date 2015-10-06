package au.com.dius.pact.matchers

import au.com.dius.pact.com.typesafe.scalalogging.StrictLogging
import au.com.dius.pact.model._
import org.json4s.{JObject, JArray, JValue, DefaultFormats}
import org.json4s.jackson.JsonMethods._

class JsonBodyMatcher extends BodyMatcher with StrictLogging {
  implicit lazy val formats = DefaultFormats

  def matchBody(expected: HttpPart, actual: HttpPart, diffConfig: DiffConfig): List[BodyMismatch] = {
    (expected.body, actual.body) match {
      case (None, None) => List()
      case (None, b) => List()
      case (a, None) => List(BodyMismatch(a, None))
      case (Some(a), Some(b)) => compare(Seq("$", "body"), parse(a), parse(b), diffConfig, expected.matchers)
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

  def compare(path: Seq[String], expected: Any, actual: Any, diffConfig: DiffConfig,
              matchers: Option[Map[String, Map[String, Any]]]): List[BodyMismatch] = {
    (expected, actual) match {
      case (a: JObject, b: JObject) => compareMaps(a.values, b.values, a, b, path, diffConfig, matchers)
      case (a: Map[String, Any], b: Map[String, Any]) => compareMaps(a, b, a, b, path, diffConfig, matchers)
      case (a: JArray, b: JArray) => compareLists(a.values, b.values, a, b, path, diffConfig, matchers)
      case (a: List[Any], b: List[Any]) => compareLists(a, b, a, b, path, diffConfig, matchers)
      case (_, _) =>
        if ((expected.isInstanceOf[JObject] && !actual.isInstanceOf[JObject]) ||
          (expected.isInstanceOf[JArray] && !actual.isInstanceOf[JArray])) {
          List(BodyMismatch(expected, actual,
            Some(s"Type mismatch: Expected ${typeOf(expected)} ${valueOf(expected)} but received ${typeOf(actual)} ${valueOf(actual)}"),
            path.mkString(".")))
        } else {
          compareValues(path, expected, actual, matchers)
        }
    }
  }

  def compareListContent(expectedValues: List[Any], actualValues: List[Any], path: Seq[String],
                         diffConfig: DiffConfig, matchers: Option[Map[String, Map[String, Any]]]) = {
    var result = List[BodyMismatch]()
    for ((value, index) <- expectedValues.view.zipWithIndex) {
      if (index < actualValues.size) {
        result = result ++: compare(path :+ index.toString, value, actualValues(index), diffConfig, matchers)
      } else if (!Matchers.matcherDefined(path, matchers)) {
        result = result :+ BodyMismatch(expectedValues, actualValues, Some(s"Expected ${valueOf(value)} but was missing"),
          path.mkString("."))
      }
    }
    result
  }

  def compareLists(expectedValues: List[Any], actualValues: List[Any], a: Any, b: Any, path: Seq[String],
                   diffConfig: DiffConfig, matchers: Option[Map[String, Map[String, Any]]]): List[BodyMismatch] = {
    if (Matchers.matcherDefined(path, matchers)) {
      logger.debug("compareLists: Matcher defined for path " + path)
      var result = Matchers.domatch[BodyMismatch](matchers, path, expectedValues, actualValues, BodyMismatchFactory)
      if (expectedValues.nonEmpty) {
        result = result ++ compareListContent(expectedValues.padTo(actualValues.length, expectedValues.head),
          actualValues, path, diffConfig, matchers)
      }
      result
    } else {
      if (expectedValues.isEmpty && actualValues.nonEmpty) {
        List(BodyMismatch(a, b, Some(s"Expected an empty List but received ${valueOf(actualValues)}"), path.mkString(".")))
      } else {
        var result = compareListContent(expectedValues, actualValues, path, diffConfig, matchers)
        if (expectedValues.size != actualValues.size) {
          result = result :+ BodyMismatch(a, b,
            Some(s"Expected a List with ${expectedValues.size} elements but received ${actualValues.size} elements"),
            path.mkString("."))
        }
        result
      }
    }
  }

  def compareMaps(expectedValues: Map[String, Any], actualValues: Map[String, Any], a: Any, b: Any, path: Seq[String],
                  diffConfig: DiffConfig, matchers: Option[Map[String, Map[String, Any]]]): List[BodyMismatch] = {
    if (expectedValues.isEmpty && actualValues.nonEmpty) {
      List(BodyMismatch(a, b, Some(s"Expected an empty Map but received ${valueOf(actualValues)}"), path.mkString(".")))
    } else {
      var result = List[BodyMismatch]()
      if (diffConfig.allowUnexpectedKeys && expectedValues.size > actualValues.size) {
        result = result :+ BodyMismatch(a, b,
          Some(s"Expected a Map with at least ${expectedValues.size} elements but received ${actualValues.size} elements"),
          path.mkString("."))
      } else if (!diffConfig.allowUnexpectedKeys && expectedValues.size != actualValues.size) {
        result = result :+ BodyMismatch(a, b,
          Some(s"Expected a Map with ${expectedValues.size} elements but received ${actualValues.size} elements"),
          path.mkString("."))
      }
      expectedValues.foreach(entry => {
        if (actualValues.contains(entry._1)) {
          result = result ++: compare(path :+ entry._1, entry._2, actualValues(entry._1), diffConfig, matchers)
        } else {
          result = result :+ BodyMismatch(a, b, Some(s"Expected ${entry._1}=${valueOf(entry._2)} but was missing"),
            path.mkString("."))
        }
      })
      result
    }
  }

  def compareValues(path: Seq[String], expected: Any, actual: Any, matchers: Option[Map[String, Map[String, Any]]]): List[BodyMismatch] = {
    if (Matchers.matcherDefined(path, matchers)) {
      logger.debug("compareValues: Matcher defined for path " + path)
      Matchers.domatch[BodyMismatch](matchers, path, expected, actual, BodyMismatchFactory)
    } else {
      logger.debug("compareValues: No matcher defined for path " + path + ", using equality")
      if (expected == actual) {
        List[BodyMismatch]()
      } else {
        List(BodyMismatch(expected, actual, Some(s"Expected ${valueOf(expected)} but received ${valueOf(actual)}"),
          path.mkString(".")))
      }
    }
  }
}
