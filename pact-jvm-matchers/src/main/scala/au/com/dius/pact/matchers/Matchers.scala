package au.com.dius.pact.matchers

import org.apache.commons.lang3.time.{DateFormatUtils, DateUtils}
import com.typesafe.scalalogging.slf4j.StrictLogging
import java.text.ParseException

import scala.collection.{JavaConverters, JavaConversions}
import java.util.Collections

object Matchers extends StrictLogging {
  def matcherDefined(path: String, matchers: Option[Map[String, Map[String, String]]]): Boolean =
    matchers.isDefined && matchers.get.contains(path)

  def domatch[Mismatch](matcherDef: Any, path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) : List[Mismatch] = {
    matcherDef match {
      case map: Map[String, String] => matcher(map).domatch[Mismatch](map, path, expected, actual, mismatchFn)
      case m =>
        logger.warn(s"Matcher $m is mis-configured, defaulting to equality matching")
        EqualsMatcher.domatch[Mismatch](Map[String, String](), path, expected, actual, mismatchFn)
    }
  }

  def matcher(matcherDef: Map[String, String]) : Matcher = {
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

  def safeToString(value: Any) = {
    if (value == null) ""
    else value.toString
  }
}

object EqualsMatcher extends Matcher {
  def domatch[Mismatch](matcherDef: Map[String, String], path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    if (Matchers.safeToString(actual).equals(expected)) {
      List[Mismatch]()
    } else {
      List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to equal ${valueOf(actual)}", path))
    }
  }
}

object RegexpMatcher extends Matcher {
  def domatch[Mismatch](matcherDef: Map[String, String], path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val regex = matcherDef.get("regex").get
    if (Matchers.safeToString(actual).matches(regex)) {
      List[Mismatch]()
    } else {
      List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to match '$regex'", path))
    }
  }
}

object TypeMatcher extends Matcher with StrictLogging {

  def matchType[Mismatch](path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
    (actual, expected) match {
      case (actual: String, expected: String) => List[Mismatch]()
      case (actual: Number, expected: Number) => List[Mismatch]()
      case (actual: Boolean, expected: Boolean) => List[Mismatch]()
      case (_, null) =>
        if (actual == null) {
          List[Mismatch]()
        } else {
          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be null", path))
        }
      case default => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be the same type as ${valueOf(expected)}", path))
    }
  }

  def matchNumber[Mismatch](path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
    (actual, expected) match {
      case (actual: Number, _) => List[Mismatch]()
      case (_, null) =>
        if (actual == null) {
          List[Mismatch]()
        } else {
          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be null", path))
        }
      case default => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be a number", path))
    }
  }

  def matchInteger[Mismatch](path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
    (actual, expected) match {
      case (actual: Integer, _) => List[Mismatch]()
      case (actual: Long, _) => List[Mismatch]()
      case (actual: BigInt, _) => List[Mismatch]()
      case (_, null) =>
        if (actual == null) {
          List[Mismatch]()
        } else {
          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be null", path))
        }
      case default => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be an integer", path))
    }
  }

  def matchReal[Mismatch](path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
    (actual, expected) match {
      case (actual: Float, _) => List[Mismatch]()
      case (actual: Double, _) => List[Mismatch]()
      case (actual: BigDecimal, _) => List[Mismatch]()
      case (_, null) =>
        if (actual == null) {
          List[Mismatch]()
        } else {
          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be null", path))
        }
      case default => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be a real number", path))
    }
  }

  def matchTimestamp[Mismatch](path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
    try {
      DateUtils.parseDate(Matchers.safeToString(actual), DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern,
        DateFormatUtils.ISO_DATETIME_FORMAT.getPattern, DateFormatUtils.SMTP_DATETIME_FORMAT.getPattern,
        "yyyy-MM-dd HH:mm:ssZZ", "yyyy-MM-dd HH:mm:ss"
      )
      List[Mismatch]()
    }
    catch {
      case e: java.text.ParseException =>
        logger.warn(s"failed to parse timestamp value of ${valueOf(actual)}", e)
        List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be a timestamp", path))
    }
  }

  def matchArray[Mismatch](path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch], matcher: String, args: List[String]) = {
    matcher match {
      case "atleast" => actual match {
        case v: List[Any] =>
          if (v.asInstanceOf[List[Any]].size < args.head.toInt) List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to have at least ${args.head} elements", path))
          else List[Mismatch]()
        case _ => List(mismatchFn.create(expected, actual, s"Array matcher $matcher can only be applied to arrays", path))
      }
      case _ => List(mismatchFn.create(expected, actual, s"Array matcher $matcher is not defined", path))
    }
  }

  def domatch[Mismatch](matcherDef: Map[String, String], path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val atleastN = "atleast\\((\\d+)\\)".r
    matcherDef.get("match") match {
      case Some("type") => matchType[Mismatch](path, expected, actual, mismatchFn)
      case Some("number") => matchNumber[Mismatch](path, expected, actual, mismatchFn)
      case Some("integer") => matchInteger[Mismatch](path, expected, actual, mismatchFn)
      case Some("real") => matchReal[Mismatch](path, expected, actual, mismatchFn)
      case Some("timestamp") => matchTimestamp[Mismatch](path, expected, actual, mismatchFn)
      case Some(atleastN(n)) => matchArray[Mismatch](path, expected, actual, mismatchFn, "atleast", List(n))
      case _ => List(mismatchFn.create(expected, actual, "type matcher is mis-configured", path))
    }
  }
}

object TimestampMatcher extends Matcher {
  def domatch[Mismatch](matcherDef: Map[String, String], path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val pattern = matcherDef.get("timestamp").get
    try {
      DateUtils.parseDate(Matchers.safeToString(actual), pattern)
      List[Mismatch]()
    } catch {
      case e: ParseException => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to match a timestamp of '$pattern': ${e.getMessage}", path))
    }
  }
}

object TimeMatcher extends Matcher {
  def domatch[Mismatch](matcherDef: Map[String, String], path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val pattern = matcherDef.get("time").get
    try {
      DateUtils.parseDate(Matchers.safeToString(actual), pattern)
      List[Mismatch]()
    } catch {
      case e: ParseException => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to match a time of '$pattern': ${e.getMessage}", path))
    }
  }
}

object DateMatcher extends Matcher {
  def domatch[Mismatch](matcherDef: Map[String, String], path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val pattern = matcherDef.get("date").get
    try {
      DateUtils.parseDate(Matchers.safeToString(actual), pattern)
      List[Mismatch]()
    } catch {
      case e: ParseException => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to match a date of '$pattern': ${e.getMessage}", path))
    }
  }
}
