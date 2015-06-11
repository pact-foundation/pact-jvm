package au.com.dius.pact.matchers

import io.gatling.jsonpath.AST._
import io.gatling.jsonpath.Parser
import org.apache.commons.lang3.time.{DateFormatUtils, DateUtils}
import com.typesafe.scalalogging.slf4j.StrictLogging
import java.text.ParseException

object Matchers extends StrictLogging {

  def matchesToken(pathElement: String, token: PathToken) = token match {
    case RootNode => if (pathElement == "$") 2 else 0
    case Field(name) => if (pathElement == name) 2 else 0
    case ArrayRandomAccess(indices) => if (pathElement.matches("\\d+") && indices.contains(pathElement.toInt)) 2 else 0
    case ArraySlice(None, None, 1) => if (pathElement.matches("\\d+")) 1 else 0
    case AnyField => 1
    case _ => 0
  }

  def matchesPath(pathExp: String, path: Seq[String]) =
    new Parser().compile(pathExp) match {
      case Parser.Success(q, _) =>
        path.corresponds(q)((pathElement, pathToken) => matchesToken(pathElement, pathToken) != 0)
      case ns: Parser.NoSuccess =>
        logger.warn(s"Path expression $pathExp is invalid, ignoring: $ns")
        false
    }

  def calculatePathWeight(pathExp: String, path: Seq[String]) = {
    new Parser().compile(pathExp) match {
      case Parser.Success(q, _) =>
        path.zip(q).map(entry => matchesToken(entry._1, entry._2)).product
      case ns: Parser.NoSuccess =>
        logger.warn(s"Path expression $pathExp is invalid, ignoring: $ns")
        0
    }
  }

  def resolveMatchers(matchers: Map[String, Map[String, String]], path: Seq[String]) =
    matchers.filterKeys(p => matchesPath(p, path))

  def matcherDefined(path: Seq[String], matchers: Option[Map[String, Map[String, String]]]): Boolean =
    matchers.isDefined && resolveMatchers(matchers.get, path).nonEmpty

  def domatch[Mismatch](matchers: Option[Map[String, Map[String, String]]], path: Seq[String], expected: Any, actual: Any,
                        mismatchFn: MismatchFactory[Mismatch]) : List[Mismatch] = {
    val matcherDef = resolveMatchers(matchers.get, path).maxBy(entry => calculatePathWeight(entry._1, path))._2
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
    } else if (matcherDef.contains("match")) {
      matcherDef("match") match {
        case "regex" => RegexpMatcher
        case "type" => TypeMatcher
        case "number" => TypeMatcher
        case "integer" => TypeMatcher
        case "real" => TypeMatcher
        case "timestamp" => TypeMatcher
        case "time" => TimeMatcher
        case "date" => DateMatcher
        case "min" => MinimumMatcher
        case "max" => MaximumMatcher
      }
    } else matcherDef.keys.head match {
      case "regex" => RegexpMatcher
      case "match" => TypeMatcher
      case "timestamp" => TimestampMatcher
      case "time" => TimeMatcher
      case "date" => DateMatcher
      case "min" => MinimumMatcher
      case "max" => MaximumMatcher
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

object EqualsMatcher extends Matcher with StrictLogging {
  def domatch[Mismatch](matcherDef: Map[String, String], path: Seq[String], expected: Any, actual: Any,
                        mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val matches: Boolean = Matchers.safeToString(actual).equals(expected)
    logger.debug(s"comparing ${valueOf(actual)} to ${valueOf(expected)} at $path -> $matches")
    if (matches) {
      List[Mismatch]()
    } else {
      List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to equal ${valueOf(actual)}", path))
    }
  }
}

object RegexpMatcher extends Matcher with StrictLogging {
  def domatch[Mismatch](matcherDef: Map[String, String], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val regex = matcherDef.get("regex").get
    val matches: Boolean = Matchers.safeToString(actual).matches(regex)
    logger.debug(s"comparing ${valueOf(actual)} with regexp $regex at $path -> $matches")
    if (matches) {
      List[Mismatch]()
    } else {
      List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to match '$regex'", path))
    }
  }
}

object TypeMatcher extends Matcher with StrictLogging {

  def matchType[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
    logger.debug(s"comparing type of ${valueOf(actual)} to ${valueOf(expected)} at $path")
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

  def matchNumber[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
    logger.debug(s"comparing type of ${valueOf(actual)} to Number at $path")
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

  def matchInteger[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
    logger.debug(s"comparing type of ${valueOf(actual)} to Integer at $path")
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

  def matchReal[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
    logger.debug(s"comparing type of ${valueOf(actual)} to Real at $path")
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

  def matchTimestamp[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
    logger.debug(s"comparing ${valueOf(actual)} as Timestamp at $path")
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

  def matchArray[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch], matcher: String, args: List[String]) = {
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

  def domatch[Mismatch](matcherDef: Map[String, String], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    matcherDef.get("match") match {
      case Some("type") => matchType[Mismatch](path, expected, actual, mismatchFn)
      case Some("number") => matchNumber[Mismatch](path, expected, actual, mismatchFn)
      case Some("integer") => matchInteger[Mismatch](path, expected, actual, mismatchFn)
      case Some("real") => matchReal[Mismatch](path, expected, actual, mismatchFn)
      case Some("timestamp") => matchTimestamp[Mismatch](path, expected, actual, mismatchFn)
      case _ => List(mismatchFn.create(expected, actual, "type matcher is mis-configured", path))
    }
  }
}

object TimestampMatcher extends Matcher with StrictLogging {
  def domatch[Mismatch](matcherDef: Map[String, String], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val pattern = matcherDef.get("timestamp").get
    logger.debug(s"comparing ${valueOf(actual)} to timestamp pattern $pattern at $path")
    try {
      DateUtils.parseDate(Matchers.safeToString(actual), pattern)
      List[Mismatch]()
    } catch {
      case e: ParseException => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to match a timestamp of '$pattern': ${e.getMessage}", path))
    }
  }
}

object TimeMatcher extends Matcher with StrictLogging {
  def domatch[Mismatch](matcherDef: Map[String, String], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val pattern = matcherDef.get("time").get
    logger.debug(s"comparing ${valueOf(actual)} to time pattern $pattern at $path")
    try {
      DateUtils.parseDate(Matchers.safeToString(actual), pattern)
      List[Mismatch]()
    } catch {
      case e: ParseException => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to match a time of '$pattern': ${e.getMessage}", path))
    }
  }
}

object DateMatcher extends Matcher with StrictLogging {
  def domatch[Mismatch](matcherDef: Map[String, String], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val pattern = matcherDef.get("date").get
    logger.debug(s"comparing ${valueOf(actual)} to date pattern $pattern at $path")
    try {
      DateUtils.parseDate(Matchers.safeToString(actual), pattern)
      List[Mismatch]()
    } catch {
      case e: ParseException => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to match a date of '$pattern': ${e.getMessage}", path))
    }
  }
}

object MinimumMatcher extends Matcher with StrictLogging {
  def domatch[Mismatch](matcherDef: Map[String, String], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val value = matcherDef.get("min").get.toInt
    logger.debug(s"comparing ${valueOf(actual)} with minimum $value at $path")
    val size = actual match {
      case v: List[Any] => v.size
      case s: String => s.length
      case n: Number => n.intValue()
    }
    if (size < value) {
      List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to have minimum $value", path))
    } else {
      List()
    }
  }
}

object MaximumMatcher extends Matcher with StrictLogging {
  def domatch[Mismatch](matcherDef: Map[String, String], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val value = matcherDef.get("max").get.toInt
    logger.debug(s"comparing ${valueOf(actual)} with maximum $value at $path")
    val size = actual match {
      case v: List[Any] => v.size
      case s: String => s.length
      case n: Number => n.intValue()
    }
    if (size > value) {
      List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to have maximum $value", path))
    } else {
      List()
    }
  }
}
