package au.com.dius.pact.model

import scala.collection.JavaConversions

object CollectionUtils {
  def javaMMapToScalaMMap(map: java.util.Map[String, java.util.Map[String, AnyRef]]) : Map[String, Map[String, Any]] = {
    JavaConversions.mapAsScalaMap(map).mapValues {
      case jmap: java.util.Map[String, _] => JavaConversions.mapAsScalaMap(jmap).toMap
    }.toMap
  }

  def javaLMapToScalaLMap(map: java.util.Map[String, java.util.List[String]]) : Map[String, List[String]] = {
    JavaConversions.mapAsScalaMap(map).mapValues {
      case jlist: java.util.List[String] => JavaConversions.collectionAsScalaIterable(jlist).toList
    }.toMap
  }

  def scalaMMapToJavaMMap(map: Map[String, Map[String, AnyRef]]) : java.util.Map[String, java.util.Map[String, AnyRef]] = {
    JavaConversions.mapAsJavaMap(map.mapValues {
      case jmap: Map[String, _] => JavaConversions.mapAsJavaMap(jmap)
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
