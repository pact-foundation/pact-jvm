package au.com.dius.pact.model

import org.json4s.{JValue, Diff}
import au.com.dius.pact.model.JsonDiff._
import org.json4s.JsonAST.JNothing

trait SharedMismatch {
  type Body = Option[JValue]
  type Headers = Map[String, String]
}

object RequestPartMismatch extends SharedMismatch {
  type Cookies = List[String]
  type Path = String
  type Method = String
}

object ResponsePartMismatch extends SharedMismatch {
  type Status = Int
}

import RequestPartMismatch._
import ResponsePartMismatch._

// Overlapping ADTs.  The body and headers can mismatch for both of them.
sealed trait RequestPartMismatch
sealed trait ResponsePartMismatch 

case class StatusMismatch(expected: Status, actual: Status) extends ResponsePartMismatch
case class HeaderMismatch(expected: Headers, actual: Headers) extends RequestPartMismatch with ResponsePartMismatch
case class BodyMismatch(diff: Diff) extends RequestPartMismatch with ResponsePartMismatch
case class CookieMismatch(expected: Cookies, actual: Cookies) extends RequestPartMismatch
case class PathMismatch(expected: Path, actual: Path) extends RequestPartMismatch
case class MethodMismatch(expected: Method, actual: Method) extends RequestPartMismatch


object Matching {
  
  def matchHeaders(expected: Option[Headers], actual: Option[Headers]): Option[HeaderMismatch] = {
    def compareHeaders(e: Map[String, String], a: Map[String, String]): Option[HeaderMismatch] = {
      def actuallyFound(kv: (String, String)): Boolean = a.get(kv._1) == Some(kv._2)
      if (e forall actuallyFound) None 
      else Some(HeaderMismatch(e, a filterKeys e.contains))
    }
    compareHeaders(expected getOrElse Map(), actual getOrElse Map())
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

  def matchBody(expected: Body, actual: Body, diffConfig: DiffConfig): Option[BodyMismatch] = {
    implicit val autoParse = JsonDiff.autoParse _
    val difference = (expected, actual) match {
      case (None, None) => noChange
      case (None, Some(b)) => if(diffConfig.structural) { noChange } else { added(b) }
      case (Some(a), None) => missing(a)
      case (Some(a), Some(b)) => diff(a, b, diffConfig)
    }
    if(difference == noChange) None
    else Some(BodyMismatch(difference))
  }

  def matchPath(expected: Path, actual: Path): Option[PathMismatch] = {
    val pathFilter = "http[s]*://([^/]*)"
    val replacedActual = actual.replaceFirst(pathFilter, "")
    if(expected == replacedActual || replacedActual.matches(expected)) None
    else Some(PathMismatch(expected, replacedActual))
  }
  
  def matchStatus(expected: Int, actual: Int): Option[StatusMismatch] = {
    if(expected == actual) None
    else Some(StatusMismatch(expected, actual))
  }
}
