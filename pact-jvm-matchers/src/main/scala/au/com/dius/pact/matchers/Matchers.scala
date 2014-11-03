package au.com.dius.pact.matchers

import org.apache.commons.lang3.time.{DateFormatUtils, DateUtils}
import com.typesafe.scalalogging.slf4j.StrictLogging
import java.text.ParseException

import scala.collection.{JavaConverters, JavaConversions}
import java.util.Collections

object Matchers extends StrictLogging {
  def matcherDefined(path: String, matchers: Option[Map[String, Any]]): Boolean =
    matchers.isDefined && matchers.get.contains(path)

  def domatch[T](matcherDef: Any, path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[T]) : List[T] = {
    val mismatches = matcherDef match {
      case map: Map[String, Any] => matcher(map).domatch[T](JavaConversions.mapAsJavaMap(map), path, expected, actual, mismatchFn)
      case m =>
        logger.warn(s"Matcher $m is mis-configured, defaulting to equality matching")
        EqualsMatcher.domatch[T](null, path, expected, actual, mismatchFn)
    }
    JavaConverters.collectionAsScalaIterableConverter(mismatches).asScala.toList
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

  def toJavaList[T](list: Seq[T]) = JavaConverters.seqAsJavaListConverter(list).asJava
}

object EqualsMatcher extends Matcher {
  def domatch[T](matcherDef: java.util.Map[String, _], path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[T]): java.util.List[T] = {
    if (actual.equals(expected)) {
      Collections.emptyList[T]()
    } else {
      Matchers.toJavaList[T](Seq(mismatchFn.create(expected, actual, s"Expected ${Matcher.valueOf(actual)} to equal ${Matcher.valueOf(actual)}", path)))
    }
  }
}

object RegexpMatcher extends Matcher {
  def domatch[T](matcherDef: java.util.Map[String, _], path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[T]): java.util.List[T] = {
    val regex = matcherDef.get("regex").toString
    if (actual.toString.matches(regex)) {
      Collections.emptyList[T]()
    } else {
      Matchers.toJavaList[T](Seq(mismatchFn.create(expected, actual, s"Expected ${Matcher.valueOf(actual)} to match '$regex'", path)))
    }
  }
}

object TypeMatcher extends Matcher with StrictLogging {

  def matchType[T](path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[T]) = {
    (actual, expected) match {
      case (actual: String, expected: String) => Collections.emptyList[T]()
      case (actual: Number, expected: Number) => Collections.emptyList[T]()
      case (actual: Boolean, expected: Boolean) => Collections.emptyList[T]()
      case (_, null) =>
        if (actual == null) {
          Collections.emptyList[T]()
        } else {
          Matchers.toJavaList[T](Seq(mismatchFn.create(expected, actual, s"Expected ${Matcher.valueOf(actual)} to be null", path)))
        }
      case default => Matchers.toJavaList[T](Seq(mismatchFn.create(expected, actual, s"Expected ${Matcher.valueOf(actual)} to be the same type as ${Matcher.valueOf(expected)}", path)))
    }
  }

  def matchTimestamp[T](path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[T]) = {
    try {
      DateUtils.parseDate(actual.toString, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern,
        DateFormatUtils.ISO_DATETIME_FORMAT.getPattern, DateFormatUtils.SMTP_DATETIME_FORMAT.getPattern,
        "yyyy-MM-dd HH:mm:ssZZ", "yyyy-MM-dd HH:mm:ss"
      )
      Collections.emptyList[T]()
    }
    catch {
      case e: java.text.ParseException =>
        logger.warn(s"failed to parse timestamp value of ${Matcher.valueOf(actual)}", e)
        Matchers.toJavaList[T](Seq(mismatchFn.create(expected, actual, s"Expected ${Matcher.valueOf(actual)} to be a timestamp", path)))
    }
  }

  def domatch[T](matcherDef: java.util.Map[String, _], path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[T]): java.util.List[T] = {
    matcherDef.get("match") match {
      case "type" => matchType[T](path, expected, actual, mismatchFn)
      case "timestamp" => matchTimestamp[T](path, expected, actual, mismatchFn)
      case _ => Matchers.toJavaList[T](Seq(mismatchFn.create(expected, actual, "type matcher is mis-configured", path)))
    }
  }
}

object TimestampMatcher extends Matcher {
  def domatch[T](matcherDef: java.util.Map[String, _], path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[T]): java.util.List[T] = {
    val pattern = matcherDef.get("timestamp").toString
    try {
      DateUtils.parseDate(actual.toString, pattern)
      Collections.emptyList[T]()
    } catch {
      case e: ParseException => Matchers.toJavaList[T](Seq(mismatchFn.create(expected, actual, s"Expected ${Matcher.valueOf(actual)} to match a timestamp of '$pattern': ${e.getMessage}", path)))
    }
  }
}

object TimeMatcher extends Matcher {
  def domatch[T](matcherDef: java.util.Map[String, _], path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[T]): java.util.List[T] = {
    val pattern = matcherDef.get("time").toString
    try {
      DateUtils.parseDate(actual.toString, pattern)
      Collections.emptyList[T]()
    } catch {
      case e: ParseException => Matchers.toJavaList[T](Seq(mismatchFn.create(expected, actual, s"Expected ${Matcher.valueOf(actual)} to match a time of '$pattern': ${e.getMessage}", path)))
    }
  }
}

object DateMatcher extends Matcher {
  def domatch[T](matcherDef: java.util.Map[String, _], path: String, expected: Any, actual: Any, mismatchFn: MismatchFactory[T]): java.util.List[T] = {
    val pattern = matcherDef.get("date").toString
    try {
      DateUtils.parseDate(actual.toString, pattern)
      Collections.emptyList[T]()
    } catch {
      case e: ParseException => Matchers.toJavaList[T](Seq(mismatchFn.create(expected, actual, s"Expected ${Matcher.valueOf(actual)} to match a date of '$pattern': ${e.getMessage}", path)))
    }
  }
}
