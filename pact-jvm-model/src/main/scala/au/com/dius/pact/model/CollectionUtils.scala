package au.com.dius.pact.model

import java.util

import scala.collection.JavaConversions

object CollectionUtils {
  def javaMMapToScalaMMap(map: java.util.Map[String, java.util.Map[String, AnyRef]]) : Map[String, Map[String, Any]] = {
    if (map != null) {
      JavaConversions.mapAsScalaMap(map).mapValues {
        case jmap: java.util.Map[String, _] => JavaConversions.mapAsScalaMap(jmap).toMap
      }.toMap
    } else {
      Map()
    }
  }

  def javaLMapToScalaLMap(map: java.util.Map[String, java.util.List[String]]) : Map[String, List[String]] = {
    if (map != null) {
      JavaConversions.mapAsScalaMap(map).mapValues {
        case jlist: java.util.List[String] => JavaConversions.collectionAsScalaIterable(jlist).toList
      }.toMap
    } else {
      Map()
    }
  }

  def scalaMMapToJavaMMap(map: Map[String, Map[String, AnyRef]]) : java.util.Map[String, java.util.Map[String, AnyRef]] = {
    JavaConversions.mapAsJavaMap(map.mapValues {
      case jmap: Map[String, _] => JavaConversions.mapAsJavaMap(jmap)
    })
  }

  def scalaLMaptoJavaLMap(map: Map[String, List[String]]): util.Map[String, util.List[String]] = {
    JavaConversions.mapAsJavaMap(map.mapValues {
      case jlist: List[String] => JavaConversions.seqAsJavaList(jlist.toSeq)
    })
  }

  def toOptionalList(list: java.util.List[String]): Option[List[String]] = {
    if (list == null) {
      None
    } else {
      Some(JavaConversions.collectionAsScalaIterable(list).toList)
    }
  }
}
