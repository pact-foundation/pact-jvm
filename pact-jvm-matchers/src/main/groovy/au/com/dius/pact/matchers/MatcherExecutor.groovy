package au.com.dius.pact.matchers

import au.com.dius.pact.model.matchingrules.Category
import au.com.dius.pact.model.matchingrules.MatchingRule
import au.com.dius.pact.model.matchingrules.RegexMatcher
import au.com.dius.pact.model.matchingrules.RuleLogic
import groovy.util.logging.Slf4j
import scala.collection.Seq
import scala.xml.Elem

@Slf4j
class MatcherExecutor {
  static <Mismatch> List<Mismatch> domatch(Category matchers, Seq<String> path, def expected, def actual,
                                           MismatchFactory<Mismatch> mismatchFn) {
    def result = matchers.allMatchingRules().collect { matchingRule ->
      domatch(matchingRule, path, expected, actual, mismatchFn)
    }

    if (matchers.ruleLogic == RuleLogic.AND) {
      result.flatten() as List<Mismatch>
    } else {
      if (result.any { it.empty }) {
        []
      } else {
        result.flatten() as List<Mismatch>
      }
    }
  }

  static String safeToString(def value) {
    if (value == null) {
      ''
    } else if (value instanceof Elem) {
      value.text
    } else {
      value as String
    }
  }

  static String valueOf(def value) {
    if (value == null) {
      'null'
    } else if (value instanceof String) {
      "'$value'"
    } else {
      value as String
    }
  }

  static <Mismatch> List<Mismatch> domatch(MatchingRule matcher, Seq<String> path, def expected, def actual,
                                           MismatchFactory<Mismatch> mismatchFn) {
    if (matcher instanceof RegexMatcher) {

    } else {
      matchEquality(path, expected, actual, mismatchFn)
    }
  }

  static <Mismatch> List<Mismatch> matchEquality(Seq<String> path, Object expected, Object actual, MismatchFactory<Mismatch> mismatchFactory) {
    def matches = safeToString(actual).equals(expected)
    log.debug("comparing ${valueOf(actual)} to ${valueOf(expected)} at $path -> $matches")
    if (matches) {
      []
    } else {
      [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to equal ${valueOf(actual)}", path) ]
    }
  }

  //object RegexpMatcher extends Matcher with StrictLogging {
//  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
//    val regex = matcherDef("regex").toString
//    val matches: Boolean = Matchers.safeToString(actual).matches(regex)
//    logger.debug(s"comparing ${valueOf(actual)} with regexp $regex at $path -> $matches")
//    if (matches) {
//      List[Mismatch]()
//    } else {
//      List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to match '$regex'", path))
//    }
//  }
//}
//
//object TypeMatcher extends Matcher with StrictLogging {
//
//  def matchType[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
//    logger.debug(s"comparing type of ${valueOf(actual)} to ${valueOf(expected)} at $path")
//    (actual, expected) match {
//      case (a: String, e: String) => List[Mismatch]()
//      case (a: Number, e: Number) => List[Mismatch]()
//      case (a: Boolean, e: Boolean) => List[Mismatch]()
//      case (a: List[_], e: List[_]) => List[Mismatch]()
//      case (a: Map[_, _], e: Map[_, _]) => List[Mismatch]()
//      case (a: Elem, e: Elem) if a.label == e.label => List[Mismatch]()
//      case (_, null) =>
//        if (actual == null) {
//          List[Mismatch]()
//        } else {
//          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be null", path))
//        }
//      case default => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be the same type as ${valueOf(expected)}", path))
//    }
//  }
//
//  def matchNumber[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
//    logger.debug(s"comparing type of ${valueOf(actual)} to Number at $path")
//    (actual, expected) match {
//      case (a: Number, _) => List[Mismatch]()
//      case (_, null) =>
//        if (actual == null) {
//          List[Mismatch]()
//        } else {
//          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be null", path))
//        }
//      case default => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be a number", path))
//    }
//  }
//
//  def matchInteger[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
//    logger.debug(s"comparing type of ${valueOf(actual)} to Integer at $path")
//    (actual, expected) match {
//      case (a: Integer, _) => List[Mismatch]()
//      case (a: Long, _) => List[Mismatch]()
//      case (a: BigInt, _) => List[Mismatch]()
//      case (_, null) =>
//        if (actual == null) {
//          List[Mismatch]()
//        } else {
//          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be null", path))
//        }
//      case default => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be an integer", path))
//    }
//  }
//
//  def matchDecimal[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
//    logger.debug(s"comparing type of ${valueOf(actual)} to Real at $path")
//    (actual, expected) match {
//      case (_: Float, _) => List[Mismatch]()
//      case (_: Double, _) => List[Mismatch]()
//      case (_: BigDecimal, _) => List[Mismatch]()
//      case (_: java.math.BigDecimal, _) => List[Mismatch]()
//      case (_, null) =>
//        if (actual == null) {
//          List[Mismatch]()
//        } else {
//          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be null", path))
//        }
//      case default => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be a decimal number", path))
//    }
//  }
//
//  def matchTimestamp[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]) = {
//    logger.debug(s"comparing ${valueOf(actual)} as Timestamp at $path")
//    try {
//      DateUtils.parseDate(Matchers.safeToString(actual), DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern,
//        DateFormatUtils.ISO_DATETIME_FORMAT.getPattern, DateFormatUtils.SMTP_DATETIME_FORMAT.getPattern,
//        "yyyy-MM-dd HH:mm:ssZZ", "yyyy-MM-dd HH:mm:ss"
//      )
//      List[Mismatch]()
//    }
//    catch {
//      case e: java.text.ParseException =>
//        logger.warn(s"failed to parse timestamp value of ${valueOf(actual)}", e)
//        List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to be a timestamp", path))
//    }
//  }
//
//  def matchArray[Mismatch](path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch], matcher: String, args: List[String]) = {
//    matcher match {
//      case "atleast" => actual match {
//        case v: List[Any] =>
//          if (v.asInstanceOf[List[Any]].size < args.head.toInt) List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to have at least ${args.head} elements", path))
//          else List[Mismatch]()
//        case _ => List(mismatchFn.create(expected, actual, s"Array matcher $matcher can only be applied to arrays", path))
//      }
//      case _ => List(mismatchFn.create(expected, actual, s"Array matcher $matcher is not defined", path))
//    }
//  }
//
//  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
//    if (matcherDef.contains("match")) {
//      matcherDef("match").toString match {
//        case "type" => matchType[Mismatch](path, expected, actual, mismatchFn)
//        case "number" => matchNumber[Mismatch](path, expected, actual, mismatchFn)
//        case "integer" => matchInteger[Mismatch](path, expected, actual, mismatchFn)
//        case "decimal" => matchDecimal[Mismatch](path, expected, actual, mismatchFn)
//        case "real" => matchDecimal[Mismatch](path, expected, actual, mismatchFn)
//        case "timestamp" => matchTimestamp[Mismatch](path, expected, actual, mismatchFn)
//        case _ => List(mismatchFn.create(expected, actual, "type matcher is mis-configured", path))
//      }
//    } else {
//      logger.warn("Matcher definition does not contain a 'match' element, defaulting to type matching")
//      matchType[Mismatch](path, expected, actual, mismatchFn)
//    }
//  }
//}
//
//object TimestampMatcher extends Matcher with StrictLogging {
//  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
//    val pattern = matcherDef.getOrElse("timestamp", "yyyy-MM-dd HH:mm:ssZZZ").toString
//    logger.debug(s"comparing ${valueOf(actual)} to timestamp pattern $pattern at $path")
//    try {
//      DateUtils.parseDate(Matchers.safeToString(actual), pattern)
//      List[Mismatch]()
//    } catch {
//      case e: ParseException => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to match a timestamp of '$pattern': ${e.getMessage}", path))
//    }
//  }
//}
//
//object TimeMatcher extends Matcher with StrictLogging {
//  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
//    val pattern = matcherDef.getOrElse("time", "HH:mm:ss").toString
//    logger.debug(s"comparing ${valueOf(actual)} to time pattern $pattern at $path")
//    try {
//      DateUtils.parseDate(Matchers.safeToString(actual), pattern)
//      List[Mismatch]()
//    } catch {
//      case e: ParseException => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to match a time of '$pattern': ${e.getMessage}", path))
//    }
//  }
//}
//
//object DateMatcher extends Matcher with StrictLogging {
//  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
//    val pattern = matcherDef.getOrElse("date", "yyyy-MM-dd").toString
//    logger.debug(s"comparing ${valueOf(actual)} to date pattern $pattern at $path")
//    try {
//      DateUtils.parseDate(Matchers.safeToString(actual), pattern)
//      List[Mismatch]()
//    } catch {
//      case e: ParseException => List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to match a date of '$pattern': ${e.getMessage}", path))
//    }
//  }
//}
//
//object MinimumMatcher extends Matcher with StrictLogging {
//  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
//    val value = matcherDef("min") match {
//      case i: Int => i
//      case j: Integer => j.toInt
//      case o => o.toString.toInt
//    }
//    logger.debug(s"comparing ${valueOf(actual)} with minimum $value at $path")
//    actual match {
//      case v: List[Any] =>
//        if (v.size < value) {
//          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to have minimum $value", path))
//        } else {
//          List()
//        }
//      case v: Elem =>
//        if (v.child.size < value) {
//          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to have minimum $value", path))
//        } else {
//          List()
//        }
//      case _ => TypeMatcher.domatch[Mismatch](matcherDef, path, expected, actual, mismatchFn)
//    }
//  }
//}
//
//object MaximumMatcher extends Matcher with StrictLogging {
//  def domatch[Mismatch](matcherDef: Map[String, Any], path: Seq[String], expected: Any, actual: Any, mismatchFn: MismatchFactory[Mismatch]): List[Mismatch] = {
//    val value = matcherDef("max") match {
//      case i: Int => i
//      case j: Integer => j.toInt
//      case o => o.toString.toInt
//    }
//    logger.debug(s"comparing ${valueOf(actual)} with maximum $value at $path")
//    actual match {
//      case v: List[Any] =>
//        if (v.size > value) {
//          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to have maximum $value", path))
//        } else {
//          List()
//        }
//      case v: Elem =>
//        if (v.child.size > value) {
//          List(mismatchFn.create(expected, actual, s"Expected ${valueOf(actual)} to have minimum $value", path))
//        } else {
//          List()
//        }
//      case _ => TypeMatcher.domatch[Mismatch](matcherDef, path, expected, actual, mismatchFn)
//    }
//  }
//}

}
