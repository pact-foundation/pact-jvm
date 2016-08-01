package au.com.dius.pact.matchers.util

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

  def toOptionalList(list: java.util.List[String]): Option[List[String]] = {
    if (list == null) {
      None
    } else {
      Some(JavaConversions.collectionAsScalaIterable(list).toList)
    }
  }
}
