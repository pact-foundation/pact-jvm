package au.com.dius.pact.consumer.specs2

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.PactVerificationResult.{Error, ExpectedButNotReceived, Mismatches, Ok, PartialMismatch, UnexpectedRequest}
import au.com.dius.pact.core.matchers._
import au.com.dius.pact.core.model.Request
import difflib.DiffUtils
import groovy.json.JsonOutput

import scala.collection.JavaConverters._

object PrettyPrinter {
  //TODO: allow configurable context lines
  val defaultContextLines = 3

  def print(mismatches: Seq[PactVerificationResult]): String = {
    mismatches.flatMap(m => {
      m match {
        case r: Ok => List()
        case r: PartialMismatch => List(PrettyPrinter.printProblem(r.getMismatches.asScala))
        case e: Mismatches => print(e.getMismatches.asScala)
        case e: Error => List(s"Test failed with an exception: ${e.getError.getMessage}")
        case u: UnexpectedRequest => printUnexpected(List(u.getRequest))
        case u: ExpectedButNotReceived => printMissing(u.getExpectedRequests.asScala)
      }
    }).mkString("\n")
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

  def printProblem(partial: Seq[Mismatch]): String = {
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

  def printMissing(missing: Seq[Request]) = {
    if(missing.isEmpty) {
      ""
    } else {
      s"missing:\n ${missing.mkString("\n")}"
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
