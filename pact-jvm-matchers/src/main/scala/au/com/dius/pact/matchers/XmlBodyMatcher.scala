package au.com.dius.pact.matchers

import au.com.dius.pact.model.{BodyMismatchFactory, BodyMismatch, DiffConfig, HttpPart}

import scala.xml._

class XmlBodyMatcher extends BodyMatcher {

  override def matchBody(expected: HttpPart, actual: HttpPart, diffConfig: DiffConfig): List[BodyMismatch] = {
    (expected.body, actual.body) match {
      case (None, None) => List[BodyMismatch]()
      case (None, b) => List[BodyMismatch]()
      case (a, None) => List(BodyMismatch(a, None))
      case (Some(a), Some(b)) => compare(Seq("$","body"), parse(a), parse(b), diffConfig, expected.matchers)
    }
  }

  def parse(xmlData: String) = {
    Utility.trim(XML.loadString(xmlData))
  }

  def appendIndex(path:Seq[String], index:Integer): Seq[String] = {
    val last = path.last
    val lastWithIndex = last + "[" + index.toString + "]"

    path.dropRight(1) :+ lastWithIndex
  }

  def appendAttribute(path:Seq[String], attribute:String) : Seq[String] = {
    path :+ attribute
  }

  def mkPathString(path:Seq[String]) = path.mkString(".")


  def compareText(path: Seq[String], expected: Node, actual: Node, config: DiffConfig,
                  matchers: Option[Map[String, Map[String, String]]]): List[BodyMismatch] = {
    if (expected.text != actual.text) {
      List(BodyMismatch(expected,actual,Some(s"Expected value '${expected.text}' but received '${actual.text}'"),mkPathString(path)))
    } else {
      List()
    }
  }

  def compare(path: Seq[String], expected: Node, actual: Node, config: DiffConfig,
                  matchers: Option[Map[String, Map[String, String]]]): List[BodyMismatch] = {
    expected match {
        case _ : Text => compareText(path,expected,actual,config,matchers)
        case _ : Elem => compareNode(path,expected,actual,config,matchers)
    }
  }
  
  def compareNode(path: Seq[String], expected: Node, actual: Node, config: DiffConfig,
                  matchers: Option[Map[String, Map[String, String]]]): List[BodyMismatch] = {
    if (actual.label != expected.label) {
      List(BodyMismatch(expected, actual, Some(s"Expected element ${expected.label} but received ${actual.label}"), mkPathString(path)))
    } else {
      val newPath = path :+ actual.label
      compareAttributes(newPath,expected,actual,config,matchers) ++ compareChildren(newPath,expected,actual,config,matchers)
    }
  }

  private def compareChildren(path: Seq[String], expected: Node, actual: Node, config: DiffConfig,
                           matchers: Option[Map[String, Map[String, String]]]): List[BodyMismatch] = {
    if (expected.child.isEmpty && actual.child.nonEmpty) {
      List(BodyMismatch(expected, actual, Some(s"Expected an empty List but received ${actual.child.mkString(",")}"),mkPathString(path)))
    } else if (expected.child.size != actual.child.size) {
      val missingChilds = expected.child.diff(actual.child)
      val result = missingChilds.map(child => BodyMismatch(expected, actual, Some(s"Expected $child but was missing"),mkPathString(path)))
      result.toList :+ BodyMismatch(expected, actual, Some(s"Expected a List with ${expected.child.size} elements but received ${actual.child.size} elements"),mkPathString(path))
    } else {
      expected.child
        .zipWithIndex
        .zip(actual.child)
        .flatMap(x => compare(appendIndex(path,x._1._2),x._1._1,x._2,config,matchers)).toList
    }
  }

  private def compareAttributes(path: Seq[String], expected: Node, actual: Node, config: DiffConfig,
                              matchers: Option[Map[String, Map[String, String]]]): List[BodyMismatch] = {
      val expectedAttrs = expected.attributes.asAttrMap
      val actualAttrs = actual.attributes.asAttrMap

      val wrongSize = if (expectedAttrs.size != actualAttrs.size) {
        List(BodyMismatch(expected, actual, Some(s"Expected a Tag with at least ${expected.attributes.size} attributes but received ${actual.attributes.size} attributes"),mkPathString(path)))
      } else {
        List()
      }

      val mismatchingAttributeValues = expectedAttrs.keys
        .map(k => (k,expectedAttrs.get(k),actualAttrs.get(k)))
        .filter(t => t._2 != t._3)

      val attributesWithWrongValue = mismatchingAttributeValues
        .filter(t => t._3.nonEmpty)
        .flatMap(t => {
          val attributePath = appendAttribute(path,t._1)
          if (Matchers.matcherDefined(attributePath,matchers)) {
            Matchers.domatch[BodyMismatch](matchers,attributePath,t._2.get,t._3.get,BodyMismatchFactory)
          } else {
            List(BodyMismatch(expected, actual, Some(s"Expected ${t._1}=${t._2.get} but received ${t._3.get}"),mkPathString(attributePath)))
          }
        })

      val attributesWithMissingValue = mismatchingAttributeValues
        .filter(t => t._3.isEmpty)
        .map(attr => {
          BodyMismatch(expected, actual, Some(s"Expected ${attr._1}=${attr._2.get} but was missing"),mkPathString(appendAttribute(path,attr._1)))
        })

      attributesWithMissingValue.toList ++ attributesWithWrongValue.toList ++ wrongSize
  }

}
