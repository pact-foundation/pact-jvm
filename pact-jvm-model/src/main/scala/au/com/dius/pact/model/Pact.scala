package au.com.dius.pact.model

import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.collection.JavaConversions

object Pact {
  def apply(provider: Provider, consumer: Consumer, interactions: java.util.List[Interaction]): Pact = {
    Pact(provider, consumer, JavaConversions.collectionAsScalaIterable(interactions).toSeq)
  }

  def from(source: String): Pact = {
    from(parse(StringInput(source)))
  }

  def from(source: JsonInput): Pact = {
    from(parse(source))
  }

  def from(json:JValue) = {
    implicit val formats = DefaultFormats
    json.transformField {
      case ("provider_state", value) => ("providerState", value)
      case ("responseMatchingRules", value) => ("matchingRules", value)
      case ("requestMatchingRules", value) => ("matchingRules", value)
      case ("body", value) =>
        if (value.isInstanceOf[JString]) ("body", value)
        else ("body", JString(pretty(value)))
      case ("method", value) => ("method", JString(value.values.toString.toUpperCase))
    }.extract[Pact]
  }

  trait MergeResult

  case class MergeSuccess(result: Pact) extends MergeResult
  case class MergeConflict(result: Seq[(Interaction, Interaction)]) extends MergeResult
}

case class Pact(provider: Provider, consumer: Consumer, interactions: Seq[Interaction]) extends PactSerializer {
  import Pact._
  
  def merge(other: Pact): MergeResult = {
    val failures: Seq[(Interaction, Interaction)] = for {
      a <- interactions
      b <- other.interactions
      if a conflictsWith b
    } yield (a, b)

    if(failures.isEmpty) {
      val mergedInteractions = interactions ++ other.interactions.filterNot(interactions.contains)
      MergeSuccess(Pact(provider, consumer, mergedInteractions))
    } else {
      MergeConflict(failures)
    }
  }
  
  def sortInteractions: Pact = 
    copy(interactions = interactions.sortBy(i => s"${i.providerState}${i.description}"))
  
  def interactionFor(description:String, providerState: Option[String]) = interactions.find { i =>
    i.description == description && i.providerState == providerState
  }
}

case class Provider(name:String)

case class Consumer(name:String)

case class Interaction(description: String,
                       providerState: Option[String],
                       request: Request,
                       response: Response) {
  
  override def toString: String = {
    s"Interaction: $description\n\tin state $providerState\nrequest:\n$request\n\nresponse:\n$response"
  }
  
  def conflictsWith(other: Interaction): Boolean = {
    description == other.description &&
    providerState == other.providerState &&
    (request != other.request || response != other.response)
  }
}

object HttpMethod {
  val Get    = "GET"
  val Post   = "POST"
  val Put    = "PUT"
  val Delete = "DELETE"
  val Head   = "HEAD"
  val Patch  = "PATCH"
}

trait HttpPart {
  def headers: Option[Map[String, String]]

  def body: Option[String]

  def mimeType = {
    headers match {
      case Some(h) =>
        if (h.contains("Content-Type"))
          h("Content-Type").split("\\s*;\\s*").head
        else
          detectContentType
      case _ => detectContentType
    }
  }

  def jsonBody = mimeType.matches("application\\/.*json")

  def matchers: Option[Map[String, Map[String, String]]]

  val xmlRegexp = "^\\s*<\\?xml\\s*version".r
  val htmlRegexp = "^\\s*(<!DOCTYPE)|(<HTML>)".r
  val jsonRegexp = "^\\s*(\"(\\.|[^\"\\\n\r])*?\"|[,:{}\\[\\]0-9.\\-+Eaeflnr-u \n\r\t])+".r
  val xmlRegexp2 = "^\\s*<\\w+\\s*(:\\w+=[\\\"”][^\\\"”]+[\\\"”])?".r

  def detectContentType = {
    body match {
      case Some(b) =>
        val s = b.substring(0, Math.min(b.length, 32))
        if (xmlRegexp.findFirstIn(s).isDefined) "application/xml"
        else if (htmlRegexp.findFirstIn(s.toUpperCase).isDefined) "text/html"
        else if (jsonRegexp.findFirstIn(s.toUpperCase).isDefined) "application/json"
        else if (xmlRegexp2.findFirstIn(s.toUpperCase).isDefined) "application/xml"
        else "text/plain"
      case _ => "text/plain"
    }
  }
}

case class Request(method: String,
                   path: String,
                   query: Option[String],
                   headers: Option[Map[String, String]],
                   body: Option[String],
                   matchingRules: Option[Map[String, Map[String, String]]]) extends HttpPart {
  def cookie: Option[List[String]] = cookieHeader.map(_._2.split(";").map(_.trim).toList)

  def headersWithoutCookie: Option[Map[String, String]] = cookieHeader match {
    case Some(cookie) => headers.map(_ - cookie._1)
    case _ => headers
  }

  private def cookieHeader = findHeaderByCaseInsensitiveKey("cookie")

  def findHeaderByCaseInsensitiveKey(key: String): Option[(String, String)] = headers.flatMap(_.find(_._1.toLowerCase == key.toLowerCase))

  override def toString: String = {
    s"\tmethod: $method\n\tpath: $path\n\tquery: $query\n\theaders: $headers\n\tmatchers: $matchers\n\tbody:\n$body"
  }

  override def matchers = matchingRules
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
    if(body == null || body.trim().size == 0) {
      None
    } else {
      Some(body)
    }
  }

  def optionalQuery(query: String): Option[String] = {
    if(query == null || query == "") {
      None
    } else {
      Some(query)
    }
  }

  def recursiveJavaMapToScalaMap(map: java.util.Map[String, Any]) : Map[String, Any] = {
    JavaConversions.mapAsScalaMap(map).mapValues {
      case jmap: java.util.Map[String, Any] => recursiveJavaMapToScalaMap(jmap)
      case v => v
    }.toMap
  }
}

object Request extends Optionals {
  def apply(method: String, path: String, query: String, headers: Map[String, String],
            body: String, matchingRules: Map[String, Map[String, String]]): Request = {
    Request(method, path, optionalQuery(query), optional(headers), optional(body), optional(matchingRules))
  }

  def apply(method: String, path: String, query: String, headers: java.util.Map[String,String], body: String,
            matchingRules: java.util.Map[String, Any]): Request = {
    Request(method, path, optionalQuery(query), optional(JavaConversions.mapAsScalaMap(headers).toMap), optional(body),
      optional(recursiveJavaMapToScalaMap(matchingRules).asInstanceOf[Map[String, Map[String, String]]]))
  }
}

case class Response(status: Int = 200,
                    headers: Option[Map[String, String]],
                    body: Option[String],
                    matchingRules: Option[Map[String, Map[String, String]]]) extends HttpPart {
  override def toString: String = {
    s"\tstatus: $status \n\theaders: $headers \n\tmatchers: $matchers \n\tbody: \n$body"
  }

  override def matchers = matchingRules
}

object Response extends Optionals {

  val CrossSiteHeaders = Map[String, String]("Access-Control-Allow-Origin" -> "*")

  def apply(status: Int, headers: Map[String, String], body: String, matchingRules: Map[String, Map[String, String]]): Response = {
    Response(status, optional(headers), optional(body), optional(matchingRules))
  }

  def apply(status: Int, headers: java.util.Map[String, String], body: String, matchingRules: java.util.Map[String, Any]): Response = {
    Response(status, optional(JavaConversions.mapAsScalaMap(headers).toMap), optional(body),
        optional(recursiveJavaMapToScalaMap(matchingRules).asInstanceOf[Map[String, Map[String, String]]]))
  }

  def invalidRequest(request: Request) = {
    Response(500, CrossSiteHeaders ++ Map("Content-Type" -> "application/json", "X-Pact-Unexpected-Request" -> "1"),
        pretty(JObject("error"-> JString(s"Unexpected request : $request"))), null)
  }
}
