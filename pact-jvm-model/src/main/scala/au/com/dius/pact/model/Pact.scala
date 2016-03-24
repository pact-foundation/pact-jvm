package au.com.dius.pact.model

import java.net.URLDecoder

import scala.collection.JavaConversions

object HttpMethod {
  val Get    = "GET"
  val Post   = "POST"
  val Put    = "PUT"
  val Delete = "DELETE"
  val Head   = "HEAD"
  val Patch  = "PATCH"
}

trait Optionals {
  def optional[A,B](map: Map[A, B]): Option[Map[A,B]] = {
    if(map == null || map.isEmpty) {
      None
    } else {
      Some(map)
    }
  }

  def optional(body: String): Option[String] = {
    if(body == null || body.trim().isEmpty) {
      None
    } else {
      Some(body)
    }
  }

  def optionalQuery(query: String, decode: Boolean = false): Option[Map[String, List[String]]] = {
    if(query == null || query == "") {
      None
    } else {
      Some(parseQueryString(query, decode))
    }
  }

  def parseQueryString(query: String, decode: Boolean): Map[String, List[String]] = {
    query.split("&").map(_.split("=")).foldLeft(Map[String, List[String]]()) {
      (m, a) =>
        val name = if (decode) URLDecoder.decode(a.head, "UTF-8") else a.head
        val value = if (decode) URLDecoder.decode(a.last, "UTF-8") else a.last
        m + (name -> (m.getOrElse(name, List[String]()) :+ value))
    }
  }

  def optionalQuery(query: Option[String], decode: Boolean): Option[Map[String, List[String]]] = {
    if(query.isDefined) {
      optionalQuery(query.get, decode)
    } else {
      None
    }
  }

  def recursiveJavaMapToScalaMap(map: java.util.Map[String, Any]) : Map[String, Any] = {
    JavaConversions.mapAsScalaMap(map).mapValues {
      case jmap: java.util.Map[String, Any] => recursiveJavaMapToScalaMap(jmap)
      case v => v
    }.toMap
  }
}
