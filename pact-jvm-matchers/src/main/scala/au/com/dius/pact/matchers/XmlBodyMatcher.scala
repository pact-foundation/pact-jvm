package au.com.dius.pact.matchers

import au.com.dius.pact.model._
import com.typesafe.scalalogging.StrictLogging

import scala.xml._

class XmlBodyMatcher extends BodyMatcher with StrictLogging {

  override def matchBody(expected: HttpPart, actual: HttpPart, diffConfig: DiffConfig): List[BodyMismatch] = {
    (expected.getBody.getState, actual.getBody.getState) match {
      case (OptionalBody.State.MISSING, _) => List()
      case (OptionalBody.State.NULL, OptionalBody.State.PRESENT) => List(BodyMismatch(None, actual.getBody.getValue,
        Some(s"Expected empty body but received '${actual.getBody.getValue}'")))
      case (OptionalBody.State.NULL, _) => List()
      case (_, OptionalBody.State.MISSING) => List(BodyMismatch(expected.getBody.getValue, None,
        Some(s"Expected body '${expected.getBody.getValue}' but was missing")))
      case (OptionalBody.State.EMPTY, OptionalBody.State.EMPTY) => List()
      case (_, _) => compareNode(Seq("$", "body"), parse(expected.getBody.orElse("")),
        parse(actual.getBody.orElse("")), diffConfig,
        Option.apply(au.com.dius.pact.matchers.util.CollectionUtils.javaMMapToScalaMMap(expected.getMatchingRules)))
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


  def compareText(path: Seq[String], expected: Node, actual: Node, config: DiffConfig,
                  matchers: Option[Map[String, Map[String, Any]]]): List[BodyMismatch] = {
    val textpath = path :+ "#text"
    val expectedText = expected.child.filter(n => n.isInstanceOf[Text]).map(n => n.text).mkString
    val actualText = actual.child.filter(n => n.isInstanceOf[Text]).map(n => n.text).mkString
    if (Matchers.matcherDefined(textpath, matchers)) {
      logger.debug("compareText: Matcher defined for path " + textpath)
      Matchers.domatch[BodyMismatch](matchers, textpath, expectedText, actualText, BodyMismatchFactory)
    } else if (expectedText != actualText) {
      List(BodyMismatch(expected, actual, Some(s"Expected value '$expectedText' but received '$actualText'"), mkPathString(textpath)))
    } else {
      List()
    }
  }

  def compareNode(path: Seq[String], expected: Node, actual: Node, config: DiffConfig,
                  matchers: Option[Map[String, Map[String, Any]]]): List[BodyMismatch] = {
    val nodePath = path :+ expected.label
    val mismatches = if (Matchers.matcherDefined(nodePath, matchers)) {
      logger.debug("compareNode: Matcher defined for path " + nodePath)
      Matchers.domatch[BodyMismatch](matchers, nodePath, expected, actual, BodyMismatchFactory)
    } else if (actual.label != expected.label) {
        List(BodyMismatch(expected, actual, Some(s"Expected element ${expected.label} but received ${actual.label}"), mkPathString(nodePath)))
    } else {
        List()
    }

    if (mismatches.isEmpty) {
      compareAttributes(nodePath, expected, actual, config, matchers) ++
        compareChildren(nodePath, expected, actual, config, matchers) ++
        compareText(nodePath, expected, actual, config, matchers)
    } else {
      mismatches
    }
  }

  private def compareChildren(path: Seq[String], expected: Node, actual: Node, config: DiffConfig,
                           matchers: Option[Map[String, Map[String, Any]]]): List[BodyMismatch] = {
    var expectedChildren = expected.child.filter(n => n.isInstanceOf[Elem])
    val actualChildren = actual.child.filter(n => n.isInstanceOf[Elem])
    val mismatches = if (Matchers.matcherDefined(path, matchers)) {
      if (expectedChildren.nonEmpty) expectedChildren = expectedChildren.padTo(actualChildren.length, expectedChildren.head)
      List()
    } else if (expected.child.isEmpty && actual.child.nonEmpty && !config.allowUnexpectedKeys) {
        List(BodyMismatch(expected, actual, Some(s"Expected an empty List but received ${actual.child.mkString(",")}"), mkPathString(path)))
    } else if (expected.child.size != actual.child.size) {
        val missingChilds = expected.child.diff(actual.child)
        val result = missingChilds.map(child => BodyMismatch(expected, actual, Some(s"Expected $child but was missing"), mkPathString(path)))
        if (config.allowUnexpectedKeys && expected.child.size > actual.child.size) {
          result.toList :+ BodyMismatch(expected, actual,
            Some(s"Expected a List with atleast ${expected.child.size} elements but received ${actual.child.size} elements"), mkPathString(path))
        } else if (!config.allowUnexpectedKeys && expected.child.size != actual.child.size) {
          result.toList :+ BodyMismatch(expected, actual,
            Some(s"Expected a List with ${expected.child.size} elements but received ${actual.child.size} elements"), mkPathString(path))
        } else {
          result.toList
        }
    } else List()

    mismatches ++: expectedChildren
        .zipWithIndex
        .zip(actualChildren)
        .flatMap(x => compareNode(appendIndex(path, x._1._2), x._1._1, x._2, config, matchers)).toList
  }

  private def compareAttributes(path: Seq[String], expected: Node, actual: Node, config: DiffConfig,
                              matchers: Option[Map[String, Map[String, Any]]]): List[BodyMismatch] = {
    val expectedAttrs = expected.attributes.asAttrMap
    val actualAttrs = actual.attributes.asAttrMap

    if (expectedAttrs.isEmpty && actualAttrs.nonEmpty && !config.allowUnexpectedKeys) {
      List(BodyMismatch(expected, actual,
        Some(s"Expected a Tag with at least ${expectedAttrs.size} attributes but received ${actual.attributes.size} attributes"),
        mkPathString(path)))
    } else {
      val mismatches = if (config.allowUnexpectedKeys && expectedAttrs.size > actualAttrs.size) {
        List(BodyMismatch(expected, actual, Some(s"Expected a Tag with at least ${expected.attributes.size} attributes but received ${actual.attributes.size} attributes"),
          mkPathString(path)))
      } else if (!config.allowUnexpectedKeys && expectedAttrs.size != actualAttrs.size) {
        List(BodyMismatch(expected, actual, Some(s"Expected a Tag with ${expected.attributes.size} attributes but received ${actual.attributes.size} attributes"),
          mkPathString(path)))
      } else {
        List()
      }

      mismatches ++ expectedAttrs.flatMap(attr => {
        if (actualAttrs.contains(attr._1)) {
          val attrPath = appendAttribute(path, attr._1)
          val actualVal = actualAttrs.get(attr._1).get
          if (Matchers.matcherDefined(attrPath, matchers)) {
            logger.debug("compareText: Matcher defined for path " + attrPath)
            Matchers.domatch[BodyMismatch](matchers, attrPath, attr._2, actualVal, BodyMismatchFactory)
          } else if (attr._2 != actualVal) {
            List(BodyMismatch(expected,actual,Some(s"Expected ${attr._1}='${attr._2}' but received $actualVal"),
              mkPathString(attrPath)))
          } else {
            List()
          }
        } else {
          List(BodyMismatch(expected, actual, Some(s"Expected ${attr._1}='${attr._2}' but was missing"),
            mkPathString(appendAttribute(path, attr._1))))
        }
      })
    }
  }

}
