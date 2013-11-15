package com.dius.pact.runner

import play.api.libs.json._
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import scala.collection.Seq
import org.scalatest.Assertions

trait JsonComparator {
  this: Assertions =>

  def compareJson(expectedString:String, actualString:String):Boolean = {
    val eTree = Json.parse(expectedString)
    val aTree = Json.parse(actualString)

    compareTrees(eTree, aTree)
  }

  def compareArray(expected: Seq[JsValue], actual:Seq[JsValue]):Boolean = {
    !expected.zip(actual).exists { case (a, b) => !compareTrees(a,b)}
  }

  implicit def objectMap(s:Seq[(String, JsValue)]) = s.toMap

  def compareObject(expected: Seq[(String, JsValue)], actual:Map[String, JsValue]):Boolean = {
    !expected.exists { case (key, value) =>
      !actual.get(key).exists(compareTrees(value, _))
    }
  }

  def compareTrees(expected:JsValue, actual:JsValue): Boolean = {
    (expected, actual) match {
      case (JsString(e), JsString(a)) => e == a
      case (JsBoolean(e), JsBoolean(a)) => e == a
      case (JsNumber(e), JsNumber(a)) => e == a
      case (JsArray(e), JsArray(a)) => compareArray(e, a)
      case (JsObject(e), JsObject(a)) => compareObject(e, a)
      case (JsNull, JsNull) => true
      case _ => false
    }
  }
}
