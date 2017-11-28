package au.com.dius.pact.matchers

import java.util.Collections

import au.com.dius.pact.model._
import au.com.dius.pact.model.matchingrules.MatchingRules
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._
import scala.xml._

class XmlBodyMatcher extends BodyMatcher with StrictLogging {

  override def matchBody(expected: HttpPart, actual: HttpPart, allowUnexpectedKeys: Boolean): java.util.List[BodyMismatch] = {
    (expected.getBody.getState, actual.getBody.getState) match {
      case (OptionalBody.State.MISSING, _) => Collections.emptyList()
      case (OptionalBody.State.NULL, OptionalBody.State.PRESENT) => Collections.singletonList(new BodyMismatch(null, actual.getBody.getValue,
        s"Expected empty body but received '${actual.getBody.getValue}'"))
      case (OptionalBody.State.NULL, _) => Collections.emptyList()
      case (_, OptionalBody.State.MISSING) => Collections.singletonList(new BodyMismatch(expected.getBody.getValue, null,
        s"Expected body '${expected.getBody.getValue}' but was missing"))
      case (OptionalBody.State.EMPTY, OptionalBody.State.EMPTY) => Collections.emptyList()
      case (_, _) => compareNode(Seq("$"), parse(expected.getBody.orElse("")),
        parse(actual.getBody.orElse("")), allowUnexpectedKeys, expected.getMatchingRules).asJava
    }
  }

  def parse(xmlData: String) = {
    if (xmlData.isEmpty) Text("")
    else Utility.trim(XML.loadString(xmlData))
  }

  def appendIndex(path:Seq[String], index:Integer): Seq[String] = {
    path :+ index.toString
  }

  def appendAttribute(path:Seq[String], attribute: String) : Seq[String] = {
    path :+ "@" + attribute
  }

  def mkPathString(path:Seq[String]) = path.mkString(".")


  def compareText(path: Seq[String], expected: Node, actual: Node, allowUnexpectedKeys: Boolean,
                  matchers: MatchingRules): List[BodyMismatch] = {
    val textpath = path :+ "#text"
    val expectedText = expected.child.filter(n => n.isInstanceOf[Text]).map(n => n.text).mkString
    val actualText = actual.child.filter(n => n.isInstanceOf[Text]).map(n => n.text).mkString
    if (Matchers.matcherDefined("body", textpath.asJava, matchers)) {
      logger.debug("compareText: Matcher defined for path " + textpath)
      Matchers.domatch[BodyMismatch](matchers, "body", textpath.asJava, expectedText, actualText, BodyMismatchFactory.INSTANCE).asScala.toList
    } else if (expectedText != actualText) {
      List(new BodyMismatch(expected, actual, s"Expected value '$expectedText' but received '$actualText'", mkPathString(textpath)))
    } else {
      List()
    }
  }

  def compareNode(path: Seq[String], expected: Node, actual: Node, allowUnexpectedKeys: Boolean,
                  matchers: MatchingRules): List[BodyMismatch] = {
    val nodePath = path :+ expected.label
    val mismatches = if (Matchers.matcherDefined("body", nodePath.asJava, matchers)) {
      logger.debug("compareNode: Matcher defined for path " + nodePath)
      Matchers.domatch[BodyMismatch](matchers, "body", nodePath.asJava, expected, actual, BodyMismatchFactory.INSTANCE).asScala.toList
    } else if (actual.label != expected.label) {
        List(new BodyMismatch(expected, actual, s"Expected element ${expected.label} but received ${actual.label}", mkPathString(nodePath)))
    } else {
        List()
    }

    if (mismatches.isEmpty) {
      compareAttributes(nodePath, expected, actual, allowUnexpectedKeys, matchers) ++
        compareChildren(nodePath, expected, actual, allowUnexpectedKeys, matchers) ++
        compareText(nodePath, expected, actual, allowUnexpectedKeys, matchers)
    } else {
      mismatches
    }
  }

  private def compareChildren(path: Seq[String], expected: Node, actual: Node, allowUnexpectedKeys: Boolean,
                           matchers: MatchingRules): List[BodyMismatch] = {
    var expectedChildren = expected.child.filter(n => n.isInstanceOf[Elem])
    val actualChildren = actual.child.filter(n => n.isInstanceOf[Elem])
    val mismatches = if (Matchers.matcherDefined("body", path.asJava, matchers)) {
      if (expectedChildren.nonEmpty) expectedChildren = expectedChildren.padTo(actualChildren.length, expectedChildren.head)
      List()
    } else if (expected.child.isEmpty && actual.child.nonEmpty && !allowUnexpectedKeys) {
        List(new BodyMismatch(expected, actual, s"Expected an empty List but received ${actual.child.mkString(",")}", mkPathString(path)))
    } else if (expected.child.size != actual.child.size) {
        val missingChilds = expected.child.diff(actual.child)
        val result = missingChilds.map(child => new BodyMismatch(expected, actual, s"Expected $child but was missing", mkPathString(path)))
        if (allowUnexpectedKeys && expected.child.size > actual.child.size) {
          result.toList :+ new BodyMismatch(expected, actual,
            s"Expected a List with atleast ${expected.child.size} elements but received ${actual.child.size} elements", mkPathString(path))
        } else if (!allowUnexpectedKeys && expected.child.size != actual.child.size) {
          result.toList :+ new BodyMismatch(expected, actual,
            s"Expected a List with ${expected.child.size} elements but received ${actual.child.size} elements", mkPathString(path))
        } else {
          result.toList
        }
    } else List()

    mismatches ++: expectedChildren
        .zipWithIndex
        .zip(actualChildren)
        .flatMap(x => compareNode(appendIndex(path, x._1._2), x._1._1, x._2, allowUnexpectedKeys, matchers)).toList
  }

  private def compareAttributes(path: Seq[String], expected: Node, actual: Node, allowUnexpectedKeys: Boolean,
                              matchers: MatchingRules): List[BodyMismatch] = {
    val expectedAttrs = expected.attributes.asAttrMap
    val actualAttrs = actual.attributes.asAttrMap

    if (expectedAttrs.isEmpty && actualAttrs.nonEmpty && !allowUnexpectedKeys) {
      List(new BodyMismatch(expected, actual,
        s"Expected a Tag with at least ${expectedAttrs.size} attributes but received ${actual.attributes.size} attributes",
        mkPathString(path)))
    } else {
      val mismatches = if (allowUnexpectedKeys && expectedAttrs.size > actualAttrs.size) {
        List(new BodyMismatch(expected, actual, s"Expected a Tag with at least ${expected.attributes.size} attributes but received ${actual.attributes.size} attributes",
          mkPathString(path)))
      } else if (!allowUnexpectedKeys && expectedAttrs.size != actualAttrs.size) {
        List(new BodyMismatch(expected, actual, s"Expected a Tag with ${expected.attributes.size} attributes but received ${actual.attributes.size} attributes",
          mkPathString(path)))
      } else {
        List()
      }

      mismatches ++ expectedAttrs.flatMap(attr => {
        if (actualAttrs.contains(attr._1)) {
          val attrPath = appendAttribute(path, attr._1)
          val actualVal = actualAttrs.get(attr._1).get
          if (Matchers.matcherDefined("body", attrPath.asJava, matchers)) {
            logger.debug("compareText: Matcher defined for path " + attrPath)
            Matchers.domatch[BodyMismatch](matchers, "body", attrPath.asJava, attr._2, actualVal, BodyMismatchFactory.INSTANCE).asScala.toList
          } else if (attr._2 != actualVal) {
            List(new BodyMismatch(expected, actual, s"Expected ${attr._1}='${attr._2}' but received $actualVal",
              mkPathString(attrPath)))
          } else {
            List()
          }
        } else {
          List(new BodyMismatch(expected, actual, s"Expected ${attr._1}='${attr._2}' but was missing",
            mkPathString(appendAttribute(path, attr._1))))
        }
      })
    }
  }

}
