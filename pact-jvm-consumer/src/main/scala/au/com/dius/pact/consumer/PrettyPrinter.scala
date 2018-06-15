package au.com.dius.pact.consumer

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.matchers.{BodyMismatch, HeaderMismatch, Mismatch}
import au.com.dius.pact.model.{RequestResponseInteraction, _}
import difflib.DiffUtils
import groovy.json.JsonOutput

@Deprecated
object PrettyPrinter {
  //TODO: allow configurable context lines
  val defaultContextLines = 3

  def print(session: PactSessionResults): String = {
    printAlmost(session.almostMatched) + printMissing(session.missing) + printUnexpected(session.unexpected)
  }

  def printDiff(label: String, expected: List[String], actual: List[String], contextLines: Int = defaultContextLines): Seq[String] = {
    import scala.collection.JavaConversions._
    val patch = DiffUtils.diff(expected, actual)
    val uDiff = DiffUtils.generateUnifiedDiff(label, "", expected, patch, contextLines)
    uDiff.toSeq
  }

  def printMapMismatch[A, B](label: String, expected: Map[A, B], actual: Map[A, B])(implicit oA: Ordering[A]): Seq[String] = {
    def stringify(m: Map[A,B]): List[String] = m.toList.sortBy(_._1).map(t => t._1+ " = " + t._2)
    printDiff(label, stringify(expected), stringify(actual))
  }

  def printStringMismatch(label: String, expected: Any, actual: Any): Seq[String] = {

    def stringify(s: String) = s.toString.split("\n").toList

    def anyToString(a: Any) : String = {
      a match {
        case None => ""
        case Some(s) => anyToString(s)
        case _ => a.toString
      }
    }

    printDiff(label, stringify(anyToString(expected)), stringify(anyToString(actual)))
  }

  def printProblem(interaction:Interaction, partial: Seq[Mismatch]): String = {
    partial.flatMap {
      case hm: HeaderMismatch => printStringMismatch("Header " + hm.getHeaderKey, hm.getExpected, hm.getActual)
      case bm: BodyMismatch => printStringMismatch("Body",
        JsonOutput.prettyPrint(bm.getExpected.toString), JsonOutput.prettyPrint(bm.getActual.toString))
      case CookieMismatch(expected, actual) => printDiff("Cookies", expected.sorted, actual.sorted)
      case PathMismatch(expected, actual, _) => printDiff("Path", List(expected), List(actual), 0)
      case MethodMismatch(expected, actual) => printDiff("Method", List(expected), List(actual), 0)
    }.mkString("\n")
  }

  def printAlmost(almost: List[PartialRequestMatch]): String = {

    def partialRequestMatch(p:PartialRequestMatch): Iterable[String] = {
      val map: Map[Interaction, Seq[Mismatch]] = p.problems
      map.flatMap {
        case (_, Nil) => None
        case (i, mismatches) => Some(printProblem(i, mismatches))
      }
    }
    almost.flatMap(partialRequestMatch).mkString("\n")
  }

  def printMissing(missing: List[Interaction]) = {
    if(missing.isEmpty) {
      ""
    } else {
      s"missing:\n ${missing.map(_.asInstanceOf[RequestResponseInteraction].getRequest).mkString("\n")}"
    }
  }

  def printUnexpected(unexpected: List[Request]) = {
    if(unexpected.isEmpty) {
      ""
    } else {
      s"unexpected:\n${unexpected.mkString("\n")}"
    }
  }

}
