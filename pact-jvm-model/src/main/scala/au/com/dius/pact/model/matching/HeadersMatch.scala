package au.com.dius.pact.model.matching

import au.com.dius.pact.model.Matching.{MatchFound, MatchResult}
import scala.collection.immutable.TreeMap
import math.Ordering

object HeadersMatch {
  private type Headers = Option[Map[String, String]]

  case class HeaderMismatch(expected: Headers, actual: Headers) extends MatchResult {
    override def toString: String = {
      s"Header Mismatch(\n\texpected: $expected\n\tactual: $actual)"
    }
  }

  def caseInsensitiveMap[T](in: Map[String, T]): TreeMap[String, T] = {
    new TreeMap[String, T]()(Ordering.by(_.toLowerCase)) ++ in
  }

  def stripWhiteSpaceAfterCommas(in: String): String = {
    in.replaceAll(",[ ]*", ",")
  }

  def compareHeaderValues(expected: String, actual: String): Boolean = {
    stripWhiteSpaceAfterCommas(expected) == stripWhiteSpaceAfterCommas(actual)
  }

  def apply(expected: Headers, actual: Headers): MatchResult = {
    def compareHeaders(e: Map[String, String], a: Map[String, String]): MatchResult = {
      def compareByKey(key: String): Boolean = { a.contains(key) && compareHeaderValues(e(key),a(key)) }
      if (e.keys forall compareByKey ) MatchFound
      else HeaderMismatch(Some(e), Some(a filterKeys e.contains))
    }
    val default = (m: Headers) => m.fold(TreeMap[String, String]())(caseInsensitiveMap)
    compareHeaders(default(expected), default(actual))
  }
  
}
