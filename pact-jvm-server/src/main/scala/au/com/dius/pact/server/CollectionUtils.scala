package au.com.dius.pact.server

import java.util

import scala.collection.JavaConverters._

object CollectionUtils {
  def javaMMapToScalaMMap(map: java.util.Map[String, java.util.Map[String, AnyRef]]) : Map[String, Map[String, Any]] = {
    if (map != null) {
      map.asScala.mapValues {
        jmap: java.util.Map[String, _] => jmap.asScala.toMap
      }.toMap
    } else {
      Map()
    }
  }

  def javaLMapToScalaLMap(map: java.util.Map[String, java.util.List[String]]) : Map[String, List[String]] = {
    if (map != null) {
      map.asScala.mapValues {
        jlist: java.util.List[String] => jlist.asScala.toList
      }.toMap
    } else {
      Map()
    }
  }

  def scalaMMapToJavaMMap(map: Map[String, Map[String, AnyRef]]) : java.util.Map[String, java.util.Map[String, AnyRef]] = {
    map.mapValues {
      jmap: Map[String, _] => jmap.asJava.asInstanceOf[java.util.Map[String, AnyRef]]
    }.asJava
  }

  def scalaLMaptoJavaLMap(map: Map[String, List[String]]): util.Map[String, util.List[String]] = {
    map.mapValues {
      jlist: List[String] => jlist.asJava
    }.asJava
  }

}
