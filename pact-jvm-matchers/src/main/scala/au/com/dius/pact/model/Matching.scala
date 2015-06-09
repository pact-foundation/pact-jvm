package au.com.dius.pact.model

import au.com.dius.pact.matchers._
import au.com.dius.pact.model.RequestPartMismatch._
import au.com.dius.pact.model.ResponsePartMismatch._

import scala.collection.immutable.TreeMap
import scala.collection.mutable
import scala.collection.breakOut

object PactConfig {
    var bodyMatchers = mutable.HashMap[String, BodyMatcher](
      "application/.*xml" -> new XmlBodyMatcher(),
      "application/.*json" -> new JsonBodyMatcher()
    )
}

trait SharedMismatch {
  type Body = Option[String]
  type Headers = Map[String, String]
  type Header = Pair[String, String]
}

object RequestPartMismatch extends SharedMismatch {
  type Cookies = List[String]
  type Path = String
  type Method = String
  type Query = String
}

object ResponsePartMismatch extends SharedMismatch {
  type Status = Int
}

// Overlapping ADTs.  The body and headers can mismatch for both of them.
sealed trait RequestPartMismatch
sealed trait ResponsePartMismatch 

case class StatusMismatch(expected: Status, actual: Status) extends ResponsePartMismatch
case class HeaderMismatch(headerKey: String, expected: String, actual: String, mismatch: Option[String] = None) extends RequestPartMismatch with ResponsePartMismatch
case class BodyTypeMismatch(expected: String, actual: String) extends RequestPartMismatch with ResponsePartMismatch
case class BodyMismatch(expected: Any, actual: Any, mismatch: Option[String] = None, path: String = "/") extends RequestPartMismatch with ResponsePartMismatch
case class CookieMismatch(expected: Cookies, actual: Cookies) extends RequestPartMismatch
case class PathMismatch(expected: Path, actual: Path, mismatch: Option[String] = None) extends RequestPartMismatch
case class MethodMismatch(expected: Method, actual: Method) extends RequestPartMismatch
case class QueryMismatch(expected: Query, actual: Query) extends RequestPartMismatch

object BodyMismatchFactory extends MismatchFactory[BodyMismatch] {
  def create(expected: scala.Any, actual: scala.Any, message: String, path: Seq[String]) =
    BodyMismatch(expected, actual, Some(message), path.mkString("."))
}

object PathMismatchFactory extends MismatchFactory[PathMismatch] {
  def create(expected: scala.Any, actual: scala.Any, message: String, path: Seq[String]) =
    PathMismatch(expected.toString, actual.toString, Some(message))
}

object HeaderMismatchFactory extends MismatchFactory[HeaderMismatch] {
  def create(expected: scala.Any, actual: scala.Any, message: String, path: Seq[String]) = {
    val expectedHeader: (String, String) = expected.asInstanceOf[Pair[String, String]]
    HeaderMismatch(expectedHeader._1, expectedHeader._2, actual.asInstanceOf[Pair[String, String]]._2, Some(message))
  }
}

object Matching {
  
  def matchHeaders(expected: Option[Headers], actual: Option[Headers], matchers: Option[Map[String, Map[String, String]]]): Seq[HeaderMismatch] = {

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

  def matchRequestHeaders(expected: Request, actual: Request) = {
    matchHeaders(expected.headersWithoutCookie, actual.headersWithoutCookie, expected.matchers)
  }

  def matchHeaders(expected: HttpPart, actual: HttpPart) : Seq[HeaderMismatch] = {
    matchHeaders(expected.headers, actual.headers, expected.matchers)
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
      val result = PactConfig.bodyMatchers.find(entry => actual.mimeType.matches(entry._1))
      if (result.isDefined) {
        result.get._2.matchBody(expected, actual, diffConfig)
      } else {
        (expected.body, actual.body) match {
          case (None, _) => List()
          case (a, None) => List(BodyMismatch(a, None))
          case (a, b) => if (a == b) List() else List(BodyMismatch(a, b))
        }
      }
    } else {
      if (expected.body == None) List()
      else List(BodyTypeMismatch(expected.mimeType, actual.mimeType))
    }
  }

  def matchPath(expected: Request, actual: Request): Option[PathMismatch] = {
    val pathFilter = "http[s]*://([^/]*)"
    val replacedActual = actual.path.replaceFirst(pathFilter, "")
    if (Matchers.matcherDefined(Seq("$", "path"), expected.matchers)) {
      val mismatch = Matchers.domatch[PathMismatch](expected.matchers, Seq("$", "path"), expected.path,
        replacedActual, PathMismatchFactory)
      mismatch.headOption
    }
    else if(expected.path == replacedActual || replacedActual.matches(expected.path)) None
    else Some(PathMismatch(expected.path, replacedActual))
  }
  
  def matchStatus(expected: Int, actual: Int): Option[StatusMismatch] = {
    if(expected == actual) None
    else Some(StatusMismatch(expected, actual))
  }

  def queryToMap(query: Query) = {
    query.split("&").map(_.split("=")).foldLeft(Map[String,Seq[String]]()) {
      (m, a) => m + (a.head -> (m.getOrElse(a.head, Seq()) :+ a.last))
    }
  }

  def matchQuery(expected: Option[Query], actual: Option[Query]): Option[QueryMismatch] = {
    (expected, actual) match {
      case (None, None) => None
      case (Some(a), None) => Some(QueryMismatch(a, ""))
      case (None, Some(b)) => Some(QueryMismatch("", b))
      case (Some(a), Some(b)) => if (queryToMap(a) == queryToMap(b)) { None } else { Some(QueryMismatch(a, b)) }
    }
  }
}
