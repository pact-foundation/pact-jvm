package au.com.dius.pact.server

import groovy.json.JsonSlurper

import scala.collection.JavaConversions

@Deprecated
object JsonUtils {

  def parseJsonString(json: String) = {
    if (json == null || json.trim.isEmpty) null
    else javaObjectGraphToScalaObjectGraph(new JsonSlurper().parseText(json))
  }

  def javaObjectGraphToScalaObjectGraph(value: AnyRef): Any = {
    value match {
      case jmap: java.util.Map[String, AnyRef] =>
        JavaConversions.mapAsScalaMap(jmap).toMap.mapValues(javaObjectGraphToScalaObjectGraph)
      case jlist: java.util.List[AnyRef] =>
        JavaConversions.collectionAsScalaIterable(jlist).map(javaObjectGraphToScalaObjectGraph).toList
      case _ => value
    }
  }

  def scalaObjectGraphToJavaObjectGraph(value: Any): Any = {
    value match {
      case map: Map[String, Any] =>
        JavaConversions.mapAsJavaMap(map.mapValues(scalaObjectGraphToJavaObjectGraph))
      case list: List[Any] =>
        JavaConversions.seqAsJavaList(list.map(scalaObjectGraphToJavaObjectGraph))
      case _ => value
    }
  }

}

