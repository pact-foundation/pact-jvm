package au.com.dius.pact.model

import java.util.Optional

import au.com.dius.pact.matchers._
import au.com.dius.pact.model.RequestPartMismatch._
import au.com.dius.pact.model.ResponsePartMismatch._
import au.com.dius.pact.model.matchingrules.MatchingRules
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConversions
import scala.collection.immutable.TreeMap

trait SharedMismatch {
  type Body = Option[String]
  type Headers = Map[String, String]
  type Header = Pair[String, String]
}

object RequestPartMismatch extends SharedMismatch {
  type Cookies = List[String]
  type Path = String
  type Method = String
  type Query = Map[String, List[String]]
}

object ResponsePartMismatch extends SharedMismatch {
  type Status = Int
}

// Overlapping ADTs.  The body and headers can mismatch for both of them.
sealed trait RequestPartMismatch {
  def description: String = toString
}

sealed trait ResponsePartMismatch

case class StatusMismatch(expected: Status, actual: Status) extends ResponsePartMismatch
case class HeaderMismatch(headerKey: String, expected: String, actual: String, mismatch: Option[String] = None)
  extends RequestPartMismatch with ResponsePartMismatch {
  override def description: String = mismatch match {
    case Some(message) => s"HeaderMismatch - $message"
    case _ => toString
  }
}
case class BodyTypeMismatch(expected: String, actual: String) extends RequestPartMismatch with ResponsePartMismatch
case class BodyMismatch(expected: Any, actual: Any, mismatch: Option[String] = None, path: String = "/")
  extends RequestPartMismatch with ResponsePartMismatch {
  override def description: String = mismatch match {
    case Some(message) => s"BodyMismatch - $message"
    case _ => toString
  }
}
case class CookieMismatch(expected: Cookies, actual: Cookies) extends RequestPartMismatch
case class PathMismatch(expected: Path, actual: Path, mismatch: Option[String] = None) extends RequestPartMismatch {
  override def description: String = mismatch match {
    case Some(message) => s"PathMismatch - $message"
    case _ => toString
  }
}
case class MethodMismatch(expected: Method, actual: Method) extends RequestPartMismatch
case class QueryMismatch(queryParameter: String, expected: String, actual: String, mismatch: Option[String] = None, path: String = "/") extends RequestPartMismatch

object BodyMismatchFactory extends MismatchFactory[BodyMismatch] {
  def create(expected: Object, actual: Object, message: String, path: Seq[String]) =
    BodyMismatch(expected, actual, Some(message), path.mkString("."))
}

object PathMismatchFactory extends MismatchFactory[PathMismatch] {
  def create(expected: Object, actual: Object, message: String, path: Seq[String]) =
    PathMismatch(expected.toString, actual.toString, Some(message))
}

object HeaderMismatchFactory extends MismatchFactory[HeaderMismatch] {
  def create(expected: Object, actual: Object, message: String, path: Seq[String]) = {
    HeaderMismatch(path.last, expected.toString, actual.toString, Some(message))
  }
}

object QueryMismatchFactory extends MismatchFactory[QueryMismatch] {
  def create(expected: Object, actual: Object, message: String, path: Seq[String]) = {
    QueryMismatch(path.last, expected.toString, actual.toString, Some(message))
  }
}

object Matching extends StrictLogging {
  
  def matchHeaders(expected: Option[Headers], actual: Option[Headers], matchers: MatchingRules): Seq[HeaderMismatch] = {

    def compareHeaders(e: Map[String, String], a: Map[String, String]): Seq[HeaderMismatch] = {
      e.foldLeft(Seq[HeaderMismatch]()) {
        (seq, values) => a.get(values._1) match {
          case Some(value) => HeaderMatcher.compareHeader(values._1, values._2, value, matchers) match {
            case Some(mismatch) => seq :+ mismatch
            case None => seq
          }
          case None => seq :+ HeaderMismatch(values._1, values._2, "", Some(s"Expected a header '${values._1}' but was missing"))
        }
      }
    }
    
    def sortedOrEmpty(h: Option[Headers]): Map[String,String] = {
      def sortCaseInsensitive[T](in: Map[String, T]): TreeMap[String, T] = {
        new TreeMap[String, T]()(Ordering.by(_.toLowerCase)) ++ in
      }
      h.fold[Map[String,String]](Map())(sortCaseInsensitive)
    }
      
    compareHeaders(sortedOrEmpty(expected), sortedOrEmpty(actual))
  }

  def javaMapToScalaMap(map: java.util.Map[String, String]) : Option[Map[String, String]] = {
    if (map == null) {
      None
    } else {
      Some(JavaConversions.mapAsScalaMap(map).toMap)
    }
  }

  def javaMapToScalaMap3(map: java.util.Map[String, java.util.List[String]]) : Option[Map[String, List[String]]] = {
    if (map == null) {
      None
    } else {
      Some(JavaConversions.mapAsScalaMap(map).mapValues {
        case jlist: java.util.List[String] => JavaConversions.collectionAsScalaIterable(jlist).toList
      }.toMap)
    }
  }

  def matchRequestHeaders(expected: Request, actual: Request) = {
    matchHeaders(javaMapToScalaMap(expected.headersWithoutCookie), javaMapToScalaMap(actual.headersWithoutCookie),
      expected.getMatchingRules)
  }

  def matchHeaders(expected: HttpPart, actual: HttpPart) : Seq[HeaderMismatch] = {
    matchHeaders(javaMapToScalaMap(expected.getHeaders), javaMapToScalaMap(actual.getHeaders),
      expected.getMatchingRules)
  }

  def matchCookie(expected: Option[Cookies], actual: Option[Cookies]): Option[CookieMismatch] = {
    def compareCookies(e: Cookies, a: Cookies) = {
      if (e forall a.contains) None 
      else Some(CookieMismatch(e, a))
    }
    compareCookies(expected getOrElse Nil, actual getOrElse Nil)
  }

  def matchMethod(expected: Method, actual: Method): Option[MethodMismatch] = {
    if(expected.equalsIgnoreCase(actual)) None
    else Some(MethodMismatch(expected, actual))
  }

  def matchBody(expected: HttpPart, actual: HttpPart, diffConfig: DiffConfig) = {
    if (expected.mimeType == actual.mimeType) {
      val result = MatchingConfig.bodyMatchers.find(entry => actual.mimeType.matches(entry._1))
      if (result.isDefined) {
        logger.debug("Found a matcher for " + actual.mimeType + " -> " + result)
        result.get._2.matchBody(expected, actual, diffConfig)
      } else {
        logger.debug("No matcher for " + actual.mimeType + ", using equality")
        (expected.getBody.getState, actual.getBody.getState) match {
          case (OptionalBody.State.MISSING, _) => List()
          case (OptionalBody.State.NULL, OptionalBody.State.PRESENT) => List(BodyMismatch(None, actual.getBody.getValue,
            Some(s"Expected empty body but received '${actual.getBody.getValue}'")))
          case (OptionalBody.State.NULL, _) => List()
          case (_, OptionalBody.State.MISSING) => List(BodyMismatch(expected.getBody.getValue, None,
            Some(s"Expected body '${expected.getBody.getValue}' but was missing")))
          case (_, _) =>
            if (expected.getBody.getValue == actual.getBody.getValue)
              List()
            else
              List(BodyMismatch(expected.getBody.getValue, actual.getBody.getValue))
        }
      }
    } else {
      if (expected.getBody.isMissing || expected.getBody.isNull || expected.getBody.isEmpty) List()
      else List(BodyTypeMismatch(expected.mimeType, actual.mimeType))
    }
  }

  def matchPath(expected: Request, actual: Request): Option[PathMismatch] = {
    val pathFilter = "http[s]*://([^/]*)"
    val replacedActual = actual.getPath.replaceFirst(pathFilter, "")
    val matchers = expected.getMatchingRules
    if (Matchers.matcherDefined("path", Seq(), matchers)) {
      val mismatch = Matchers.domatch[PathMismatch](matchers, "path", Seq(), expected.getPath,
        replacedActual, PathMismatchFactory)
      mismatch.headOption
    }
    else if(expected.getPath == replacedActual || replacedActual.matches(expected.getPath)) None
    else Some(PathMismatch(expected.getPath, replacedActual))
  }
  
  def matchStatus(expected: Integer, actual: Integer): Option[StatusMismatch] = {
    if(expected == actual) None
    else Some(StatusMismatch(expected, actual))
  }

  def matchQuery(expected: Request, actual: Request) = {
    javaMapToScalaMap3(expected.getQuery).getOrElse(Map()).foldLeft(Seq[QueryMismatch]()) {
      (seq, values) => javaMapToScalaMap3(actual.getQuery).getOrElse(Map()).get(values._1) match {
        case Some(value) => seq ++ QueryMatcher.compareQuery(values._1, values._2, value, expected.getMatchingRules)
        case None => seq :+ QueryMismatch(values._1, values._2.mkString(","), "",
          Some(s"Expected query parameter '${values._1}' but was missing"), Seq("$", "query", values._1).mkString("."))
      }
    } ++ javaMapToScalaMap3(actual.getQuery).getOrElse(Map()).foldLeft(Seq[QueryMismatch]()) {
      (seq, values) => javaMapToScalaMap3(expected.getQuery).getOrElse(Map()).get(values._1) match {
        case Some(value) => seq
        case None => seq :+ QueryMismatch(values._1, "", values._2.mkString(","),
          Some(s"Unexpected query parameter '${values._1}' received"), Seq("$", "query", values._1).mkString("."))
      }
    }
  }
}
