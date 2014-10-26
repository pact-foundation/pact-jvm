package au.com.dius.pact.matchers

import au.com.dius.pact.model.BodyMismatch
import scala.collection.mutable
import org.apache.commons.lang3.time.{FastDateParser, DateFormatUtils, DateUtils}
import com.typesafe.scalalogging.slf4j.StrictLogging
import java.text.ParseException

object Matchers extends StrictLogging {
  def matcherDefined(path: String, matchers: Option[Map[String, Any]]): Boolean =
    matchers.isDefined && matchers.get.contains(path)

  def domatch(matcherDef: Any, path: String, expected: Any, actual: Any) : List[BodyMismatch] = {
    matcherDef match {
      case map: Map[String, Any] => matcher(map).domatch(map, path, expected, actual)
      case m =>
        logger.warn(s"Matcher $m is mis-configured, defaulting to equality matching")
        EqualsMatcher.domatch(Map(), path, expected, actual)
    }
  }

  def matcher(matcherDef: Map[String, Any]) : Matcher = {
    if (matcherDef.isEmpty) {
      logger.warn(s"Unrecognised empty matcher, defaulting to equality matching")
      EqualsMatcher
    } else matcherDef.keys.head match {
      case "regex" => RegexpMatcher
      case "match" => TypeMatcher
      case "timestamp" => TimestampMatcher
      case "time" => TimeMatcher
      case "date" => DateMatcher
      case m =>
        logger.warn(s"Unrecognised matcher $m, defaulting to equality matching")
        EqualsMatcher
    }
  }

}

trait Matcher {
  def domatch(matcherDef: Map[String, Any], path: String, expected: Any, actual: Any) : List[BodyMismatch]

  def valueOf(value: Any) = {
    value match {
      case s: String => s"'$value'"
      case null => "null"
      case _ => value.toString
    }
  }
}

object EqualsMatcher extends Matcher {
  def domatch(matcherDef: Map[String, Any], path: String, expected: Any, actual: Any): List[BodyMismatch] = {
    if (actual.equals(expected)) {
      List()
    } else {
      List(BodyMismatch(expected, actual, Some(s"Expected ${valueOf(actual)} to equal ${valueOf(actual)}"), path))
    }
  }
}

object RegexpMatcher extends Matcher {
  def domatch(matcherDef: Map[String, Any], path: String, expected: Any, actual: Any): List[BodyMismatch] = {
    val regex = matcherDef("regex").toString
    if (actual.toString.matches(regex)) {
      List()
    } else {
      List(BodyMismatch(expected, actual, Some(s"Expected ${valueOf(actual)} to match '$regex'"), path))
    }
  }
}

object TypeMatcher extends Matcher with StrictLogging {

  def matchType(path: String, expected: Any, actual: Any) = {
    (actual, expected) match {
      case (actual: String, expected: String) => List()
      case (actual: Number, expected: Number) => List()
      case (actual: Boolean, expected: Boolean) => List()
      case (_, null) =>
        if (actual == null) {
          List()
        } else {
          List(BodyMismatch(expected, actual, Some(s"Expected ${valueOf(actual)} to be null"), path))
        }
      case default => List(BodyMismatch(expected, actual, Some(s"Expected ${valueOf(actual)} to be the same type as ${valueOf(expected)}"), path))
    }
  }

  def matchTimestamp(path: String, expected: Any, actual: Any) = {
    try {
      DateUtils.parseDate(actual.toString, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern,
        DateFormatUtils.ISO_DATETIME_FORMAT.getPattern, DateFormatUtils.SMTP_DATETIME_FORMAT.getPattern,
        "yyyy-MM-dd HH:mm:ssZZ", "yyyy-MM-dd HH:mm:ss"
      )
      List()
    }
    catch {
      case e: java.text.ParseException =>
        logger.warn(s"failed to parse timestamp value of ${valueOf(actual)}", e)
        List(BodyMismatch(expected, actual, Some(s"Expected ${valueOf(actual)} to be a timestamp"), path))
    }
  }

  def domatch(matcherDef: Map[String, Any], path: String, expected: Any, actual: Any): List[BodyMismatch] = {
    matcherDef("match") match {
      case "type" => matchType(path, expected, actual)
      case "timestamp" => matchTimestamp(path, expected, actual)
      case _ => List(BodyMismatch(expected, actual, Some("type matcher is mis-configured"), path))
    }
  }
}

object TimestampMatcher extends Matcher {
  def domatch(matcherDef: Map[String, Any], path: String, expected: Any, actual: Any): List[BodyMismatch] = {
    val pattern = matcherDef("timestamp").toString
    try {
      DateUtils.parseDate(actual.toString, pattern)
      List()
    } catch {
      case e: ParseException => List(BodyMismatch(expected, actual, Some(s"Expected ${valueOf(actual)} to match a timestamp of '$pattern': ${e.getMessage}"), path))
    }
  }
}

object TimeMatcher extends Matcher {
  def domatch(matcherDef: Map[String, Any], path: String, expected: Any, actual: Any): List[BodyMismatch] = {
    val pattern = matcherDef("time").toString
    try {
      DateUtils.parseDate(actual.toString, pattern)
      List()
    } catch {
      case e: ParseException => List(BodyMismatch(expected, actual, Some(s"Expected ${valueOf(actual)} to match a time of '$pattern': ${e.getMessage}"), path))
    }
  }
}

object DateMatcher extends Matcher {
  def domatch(matcherDef: Map[String, Any], path: String, expected: Any, actual: Any): List[BodyMismatch] = {
    val pattern = matcherDef("date").toString
    try {
      DateUtils.parseDate(actual.toString, pattern)
      List()
    } catch {
      case e: ParseException => List(BodyMismatch(expected, actual, Some(s"Expected ${valueOf(actual)} to match a date of '$pattern': ${e.getMessage}"), path))
    }
  }
}
