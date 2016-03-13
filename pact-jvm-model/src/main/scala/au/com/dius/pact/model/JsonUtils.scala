package au.com.dius.pact.model

import java.io.File

import groovy.json.JsonSlurper

import scala.collection.JavaConversions

object JsonUtils {
  def parseJson(file: File) = {
    javaObjectGraphToScalaObjectGraph(new JsonSlurper().parse(file))
  }

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

}
