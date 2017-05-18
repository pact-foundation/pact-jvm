package au.com.dius.pact.matchers

import au.com.dius.pact.com.typesafe.scalalogging.StrictLogging
import io.gatling.jsonpath.AST._
import io.gatling.jsonpath.Parser
import org.apache.commons.lang3.time.{DateFormatUtils, DateUtils}
import java.text.ParseException

import scala.xml.Elem

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
        val filter = path.reverse.tails.filter(l =>
          l.reverse.corresponds(q)((pathElement, pathToken) => matchesToken(pathElement, pathToken) != 0))
        if (filter.nonEmpty) {
          filter.maxBy(seq => seq.length).length
        } else {
          0
        }
      case ns: Parser.NoSuccess =>
        logger.warn(s"Path expression $pathExp is invalid, ignoring: $ns")
        0
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

  def resolveMatchers(matchers: Map[String, Map[String, Any]], path: Seq[String]) =
    matchers.filterKeys(p => matchesPath(p, path) > 0)

  def matcherDefined(path: Seq[String], matchers: Option[Map[String, Map[String, Any]]]): Boolean =
    matchers.isDefined && resolveMatchers(matchers.get, path).nonEmpty

  def wildcardMatcherDefined(path: Seq[String], matchers: Option[Map[String, Map[String, Any]]]): Boolean = {
    matchers match {
      case Some(m) => {
        val resolvedMatchers = m.filterKeys(p => matchesPath(p, path) == path.length)
        resolvedMatchers.exists(entry => entry._1.endsWith(".*"))
      }
      case None => false
    }
  }

  def domatch[Mismatch](matchers: Option[Map[String, Map[String, Any]]], path: Seq[String], expected: Any, actual: Any,
                        mismatchFn: MismatchFactory[Mismatch]) : List[Mismatch] = {
    val matcherDef = selectBestMatcher(matchers, path)
    matcherDef match {
      case map: Map[String, Any] => matcher(map).domatch[Mismatch](map, path, expected, actual, mismatchFn)
      case m =>
        logger.warn(s"Matcher $m is mis-configured, defaulting to equality matching")
        EqualsMatcher.domatch[Mismatch](Map[String, String](), path, expected, actual, mismatchFn)
    }
  }

  def selectBestMatcher[Mismatch](matchers: Option[Map[String, Map[String, Any]]], path: Seq[String]): Map[String, Any] = {
    resolveMatchers(matchers.get, path).maxBy(entry => calculatePathWeight(entry._1, path))._2
  }

  def matcher(matcherDef: Map[String, Any]) : Matcher = {
    if (matcherDef.isEmpty) {
      logger.warn(s"Unrecognised empty matcher, defaulting to equality matching")
      EqualsMatcher
    } else if (matcherDef.contains("match")) {
      matcherDef("match") match {
        case "regex" => RegexpMatcher
        case "type" =>
          if (matcherDef.contains("min")) MinimumMatcher
          else if (matcherDef.contains("max")) MaximumMatcher
          else TypeMatcher
        case "number" => TypeMatcher
        case "integer" => TypeMatcher
        case "real" => TypeMatcher
        case "decimal" => TypeMatcher
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
    else value match {
      case elem: Elem => elem.text
      case _ => value.toString
    }
  }
}

object EqualsMatcher extends Matcher with StrictLogging {
  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any,
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
  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val regex = matcherDef.get("regex").get.toString
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
      case (a: String, e: String) => List[Mismatch]()
      case (a: Number, e: Number) => List[Mismatch]()
      case (a: Boolean, e: Boolean) => List[Mismatch]()
      case (a: List[_], e: List[_]) => List[Mismatch]()
      case (a: Map[_, _], e: Map[_, _]) => List[Mismatch]()
      case (a: Elem, e: Elem) if a.label == e.label => List[Mismatch]()
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
      case (a: Number, _) => List[Mismatch]()
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
      case (a: Integer, _) => List[Mismatch]()
      case (a: Long, _) => List[Mismatch]()
      case (a: BigInt, _) => List[Mismatch]()
      case (_, null) =>
        if (actual == null) {
          List[Mismatch]()
        } else {
          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be null", path))
        }
      case default => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be an integer", path))
    }
  }

  def matchDecimal[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
    logger.debug(s"comparing type of ${valueOf(actual)} to Real at $path")
    (actual, expected) match {
      case (_: Float, _) => List[Mismatch]()
      case (_: Double, _) => List[Mismatch]()
      case (_: BigDecimal, _) => List[Mismatch]()
      case (_: java.math.BigDecimal, _) => List[Mismatch]()
      case (_, null) =>
        if (actual == null) {
          List[Mismatch]()
        } else {
          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be null", path))
        }
      case default => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be a decimal number", path))
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

  def matchTime[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
    logger.debug(s"comparing ${valueOf(actual)} as Time at $path")
    try {
      DateUtils.parseDate(Matchers.safeToString(actual), DateFormatUtils.ISO_TIME_TIME_ZONE_FORMAT.getPattern,
        DateFormatUtils.ISO_TIME_FORMAT.getPattern, DateFormatUtils.ISO_TIME_NO_T_FORMAT.getPattern)
      List[Mismatch]()
    }
    catch {
      case e: java.text.ParseException =>
        logger.warn(s"failed to parse time value of ${valueOf(actual)}", e)
        List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be a time", path))
    }
  }

  def matchDate[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
    logger.debug(s"comparing ${valueOf(actual)} as Date at $path")
    try {
      DateUtils.parseDate(Matchers.safeToString(actual), DateFormatUtils.ISO_DATE_FORMAT.getPattern,
        DateFormatUtils.ISO_DATE_TIME_ZONE_FORMAT.getPattern)
      List[Mismatch]()
    }
    catch {
      case e: java.text.ParseException =>
        logger.warn(s"failed to parse date value of ${valueOf(actual)}", e)
        List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be a date", path))
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

  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    if (matcherDef.contains("match")) {
      matcherDef.get("match").get.toString match {
        case "type" => matchType[Mismatch](path, expected, actual, mismatchFn)
        case "number" => matchNumber[Mismatch](path, expected, actual, mismatchFn)
        case "integer" => matchInteger[Mismatch](path, expected, actual, mismatchFn)
        case "decimal" => matchDecimal[Mismatch](path, expected, actual, mismatchFn)
        case "real" => matchDecimal[Mismatch](path, expected, actual, mismatchFn)
        case "timestamp" => if (matcherDef.contains("timestamp"))
            TimestampMatcher.domatch(matcherDef, path, expected, actual, mismatchFn)
          else
            matchTimestamp[Mismatch](path, expected, actual, mismatchFn)
        case "time" => if (matcherDef.contains("time"))
            TimeMatcher.domatch(matcherDef, path, expected, actual, mismatchFn)
          else
            matchTime[Mismatch](path, expected, actual, mismatchFn)
        case "date" => if (matcherDef.contains("date"))
            DateMatcher.domatch(matcherDef, path, expected, actual, mismatchFn)
          else
            matchDate[Mismatch](path, expected, actual, mismatchFn)
        case _ => List(mismatchFn.create(expected, actual, "type matcher is mis-configured", path))
      }
    } else {
      logger.warn("Matcher definition does not contain a 'match' element, defaulting to type matching")
      matchType[Mismatch](path, expected, actual, mismatchFn)
    }
  }
}

object TimestampMatcher extends Matcher with StrictLogging {
  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val pattern = matcherDef.get("timestamp").get.toString
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
  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val pattern = matcherDef.get("time").get.toString
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
  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val pattern = matcherDef.get("date").get.toString
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
  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val value = matcherDef.get("min").get match {
      case i: Int => i
      case j: Integer => j.toInt
      case o => o.toString.toInt
    }
    logger.debug(s"comparing ${valueOf(actual)} with minimum $value at $path")
    actual match {
      case v: List[Any] =>
        if (v.size < value) {
          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to have minimum $value", path))
        } else {
          List()
        }
      case v: Elem =>
        if (v.child.size < value) {
          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to have minimum $value", path))
        } else {
          List()
        }
      case _ => TypeMatcher.domatch[Mismatch](matcherDef, path, expected, actual, mismatchFn)
    }
  }
}

object MaximumMatcher extends Matcher with StrictLogging {
  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
    val value = matcherDef.get("max").get match {
      case i: Int => i
      case j: Integer => j.toInt
      case o => o.toString.toInt
    }
    logger.debug(s"comparing ${valueOf(actual)} with maximum $value at $path")
    actual match {
      case v: List[Any] =>
        if (v.size > value) {
          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to have maximum $value", path))
        } else {
          List()
        }
      case v: Elem =>
        if (v.child.size > value) {
          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to have minimum $value", path))
        } else {
          List()
        }
      case _ => TypeMatcher.domatch[Mismatch](matcherDef, path, expected, actual, mismatchFn)
    }
  }
}
