package au.com.dius.pact.server

import au.com.dius.pact.core.support.Json
import com.google.gson.JsonParser

import scala.collection.JavaConverters._

object JsonUtils {

  def parseJsonString(json: String) = {
    if (json == null || json.trim.isEmpty) null
    else javaObjectGraphToScalaObjectGraph(Json.INSTANCE.toMap(new JsonParser().parse(json)))
  }

  def javaObjectGraphToScalaObjectGraph(value: AnyRef): Any = {
    value match {
      case jmap: java.util.Map[String, AnyRef] =>
        jmap.asScala.toMap.mapValues(javaObjectGraphToScalaObjectGraph)
      case jlist: java.util.List[AnyRef] =>
        jlist.asScala.map(javaObjectGraphToScalaObjectGraph).toList
      case _ => value
    }
  }

  def scalaObjectGraphToJavaObjectGraph(value: Any): Any = {
    value match {
      case map: Map[String, Any] =>
        map.mapValues(scalaObjectGraphToJavaObjectGraph).asJava
      case list: List[Any] =>
        list.map(scalaObjectGraphToJavaObjectGraph).asJava
      case _ => value
    }
  }

}
