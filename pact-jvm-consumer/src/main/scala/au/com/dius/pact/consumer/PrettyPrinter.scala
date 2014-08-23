package au.com.dius.pact.consumer

import au.com.dius.pact.model._
import au.com.dius.pact.model.Interaction
import scala.Some
import difflib.DiffUtils
import org.json4s.JValue

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

  def printStringMismatch(label: String, expected: Option[JValue], actual: Option[JValue]): Seq[String] = {
    import org.json4s.jackson.JsonMethods._
    def stringify(s: Option[JValue]) = s.fold(List[String]()){j => pretty(render(j)).split("\n").toList}
    printDiff(label, stringify(expected), stringify(actual))
  }

  def printProblem(interaction:Interaction, partial: Seq[RequestPartMismatch]): String = {
    partial.flatMap {
      case HeaderMismatch(expected, actual) => printMapMismatch("Headers", expected, actual)
      case BodyMismatch(expected, actual) => printStringMismatch("Body", expected, actual)
      case CookieMismatch(expected, actual) => printDiff("Cookies", expected.sorted, actual.sorted)
      case PathMismatch(expected, actual) => printDiff("Path", List(expected), List(actual), 0)
      case MethodMismatch(expected, actual) => printDiff("Method", List(expected), List(actual), 0)
    }.mkString("\n")
  }

  def printAlmost(almost: List[PartialRequestMatch]): String = {

    def partialRequestMatch(p:PartialRequestMatch): Iterable[String] = {
      val map: Map[Interaction, Seq[RequestPartMismatch]] = p.problems
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
      s"missing:\n ${missing.map(_.request).mkString("\n")}"
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
