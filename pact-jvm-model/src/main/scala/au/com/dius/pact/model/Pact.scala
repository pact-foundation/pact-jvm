package au.com.dius.pact.model

import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.collection.{mutable, JavaConversions}

object PactConfig {
  var bodyMatchers = mutable.HashMap[String, BodyMatcher]("application/json" -> new JsonBodyMatcher())
}

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
      case ("body", value) => ("body", JString(pretty(value)))
    }.extract[Pact]
  }

  trait MergeResult

  case class MergeSuccess(result: Pact) extends MergeResult
  case class MergeConflict(result: Seq[(Interaction, Interaction)]) extends MergeResult
}

@deprecated("Use PactFragment where possible, same functionality but more appropriate language.  The serialized file is the true pact")
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
  def mimeType = headers.getOrElse(Map()).getOrElse("Content-Type", "application/json").split("\\s*;\\s*").head
  def jsonBody = mimeType == "application/json"
}

case class Request(method: String,
                   path: String,
                   query: Option[String],
                   headers: Option[Map[String, String]],
                   body: Option[String],
                   matchers: Option[Map[String, Any]]) extends HttpPart {
  def cookie: Option[List[String]] = cookieHeader.map(_._2.split(";").map(_.trim).toList)

  def headersWithoutCookie: Option[Map[String, String]] = cookieHeader match {
    case Some(cookie) => headers.map(_ - cookie._1)
    case _ => headers
  }

  private def cookieHeader = findHeaderByCaseInsensitiveKey("cookie")

  private def findHeaderByCaseInsensitiveKey(key: String): Option[(String, String)] = headers.flatMap(_.find(_._1.toLowerCase == key.toLowerCase))

  override def toString: String = {
    s"\tmethod: $method\n\tpath: $path\n\tquery: $query\n\theaders: $headers\n\trequestMatchers: $matchers\n\tbody:\n$body"
  }
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
}

object Request extends Optionals {
  def apply(method: String, path: String, query: String, headers: Map[String, String],
            body: String, matchers: Map[String, Any]): Request = {
    Request(method, path, optionalQuery(query), optional(headers), optional(body), optional(matchers))
  }

  def apply(method: String, path: String, query: String, headers: java.util.Map[String,String], body: String,
            matchers: java.util.Map[String,Any]): Request = {
    Request(method, path, optionalQuery(query), optional(JavaConversions.mapAsScalaMap(headers).toMap), optional(body),
      optional(JavaConversions.mapAsScalaMap(matchers).toMap))
  }
}

case class Response(status: Int,
                    headers: Option[Map[String, String]],
                    body: Option[String],
                    matchers: Option[Map[String, Any]]) extends HttpPart {
  override def toString: String = {
    s"\tstatus: $status \n\theaders: $headers \n\tmatchers: $matchers \n\tbody: \n$body"
  }
}

object Response extends Optionals {

  val CrossSiteHeaders = Map[String, String]("Access-Control-Allow-Origin" -> "*")

  def apply(status: Int, headers: Map[String, String], body: String, matchers: Map[String, Any]): Response = {
    Response(status, optional(headers), optional(body), optional(matchers))
  }

  def apply(status: Int, headers: java.util.Map[String, String], body: String, matchers: java.util.Map[String, Any]): Response = {
    Response(status, optional(JavaConversions.mapAsScalaMap(headers).toMap), optional(body),
        optional(JavaConversions.mapAsScalaMap(matchers).toMap))
  }

  def invalidRequest(request: Request) = {
    Response(500, CrossSiteHeaders ++ Map("Content-Type" -> "application/json", "X-Pact-Unexpected-Request" -> "1"),
        pretty(JObject("error"-> JString(s"Unexpected request : $request"))), null)
  }
}
