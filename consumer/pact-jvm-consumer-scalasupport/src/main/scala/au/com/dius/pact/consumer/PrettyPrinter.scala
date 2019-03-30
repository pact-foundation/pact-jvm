package au.com.dius.pact.consumer

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.matchers.{BodyMismatch, HeaderMismatch, Mismatch}
import au.com.dius.pact.core.model.{RequestResponseInteraction, _}
import au.com.dius.pact.core.matchers.{CookieMismatch, MethodMismatch, PartialRequestMatch, PathMismatch}
import difflib.DiffUtils
import groovy.json.JsonOutput
import scala.collection.JavaConverters._

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
      case cm: CookieMismatch => printDiff("Cookies", asScalaBuffer(cm.getExpected).toList.sorted,
        asScalaBuffer(cm.getActual).toList.sorted)
      case pm: PathMismatch => printDiff("Path", List(pm.getExpected), List(pm.getActual), 0)
      case mm: MethodMismatch => printDiff("Method", List(mm.getExpected), List(mm.getActual), 0)
    }.mkString("\n")
  }

  def printAlmost(almost: List[PartialRequestMatch]): String = {

    def partialRequestMatch(p: PartialRequestMatch): Iterable[String] = {
      val map = p.getProblems.asScala.mapValues(_.asScala.toSeq)
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
