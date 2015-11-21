package au.com.dius.pact.model

import groovy.json.JsonSlurper

import scala.collection.JavaConversions

object JsonUtils {

  def parseJsonString(json: String) = {
    javaObjectGraphToScalaObjectGraph(new JsonSlurper().parseText(json))
  }

  def javaObjectGraphToScalaObjectGraph(value: AnyRef): Any = {
    value match {
      case jmap: java.util.Map[AnyRef, AnyRef] =>
        JavaConversions.mapAsScalaMap(jmap).mapValues(javaObjectGraphToScalaObjectGraph)
      case jlist: java.util.List[AnyRef] =>
        JavaConversions.collectionAsScalaIterable(jlist).map(javaObjectGraphToScalaObjectGraph).toList
      case _ => value
    }
  }

}
