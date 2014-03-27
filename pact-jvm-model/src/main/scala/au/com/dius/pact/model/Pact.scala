package au.com.dius.pact.model

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import scala.collection.JavaConversions

object Pact {
  def apply(provider: Provider, consumer: Consumer, interactions: java.util.List[Interaction]): Pact = {
    Pact(provider, consumer, JavaConversions.collectionAsScalaIterable(interactions).toSeq)
  }

  def from(source: JsonInput): Pact = {
    from(parse(source))
  }

  def from(json:JValue) = {
    implicit val formats = DefaultFormats
    json.transformField { case ("provider_state", value) => ("providerState", value)}.extract[Pact]
  }

  def merge(a: Pact, b: Pact): MergeResult = {
    val failures: Seq[(Interaction, Interaction)] = a.interactions.flatMap { ai =>
      b.interactions.find { bi =>
        ai.description == bi.description &&
        ai.providerState == bi.providerState &&
          (ai.request != bi.request || ai.response != bi.response)
      }.map[(Interaction, Interaction)]( ai -> _ )
    }
    if(failures.isEmpty) {
      val mergedInteractions = a.interactions ++ b.interactions.filterNot { bi => a.interactions.contains(bi) }
      MergeSuccess(Pact(a.provider, a.consumer, mergedInteractions))
    } else {
      ConflictingInteractions(failures)
    }
  }

  trait MergeResult

  case class MergeSuccess(result: Pact) extends MergeResult
  case class ConflictingInteractions(result: Seq[(Interaction, Interaction)]) extends MergeResult
}

case class Pact(provider:Provider, consumer:Consumer, interactions: Seq[Interaction]) extends PactSerializer {
  def interactionFor(description:String, providerState:String) = interactions.find { i =>
    i.description == description && i.providerState == providerState
  }
}

case class Provider(name:String)

case class Consumer(name:String)

case class Interaction(
                        description: String,
                        providerState: String,
                        request: Request,
                        response: Response) {
  override def toString: String = {
    s"Interaction: $description\n\tin state $providerState\nrequest:\n$request\n\nresponse:\n$response"
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

//TODO: support duplicate headers
case class Request(method: String,
                   path: String,
                   headers: Option[Map[String, String]],
                   body: Option[JValue]) {
  def bodyString:Option[String] = body.map{ b => compact(render(b))}

  def cookie: Option[List[String]] = cookieHeader.map(_._2.split(";").map(_.trim).toList)

  def headersWithoutCookie: Option[Map[String, String]] = cookieHeader match {
    case Some(cookie) => headers.map(_ - cookie._1)
    case _ => headers
  }

  private def cookieHeader = findHeaderByCaseInsensitiveKey("cookie")

  private def findHeaderByCaseInsensitiveKey(key: String): Option[(String, String)] = headers.flatMap(_.find(_._1.toLowerCase == key.toLowerCase))

  override def toString: String = {
    s"\tmethod: $method\n\tpath: $path\n\theaders: $headers\n\tbody:\n${body.map{b => pretty(render(b))}}"
  }
}

trait Optionals {
  def optional(headers: Map[String, String]): Option[Map[String,String]] = {
    if(headers == null | headers.isEmpty) {
      None
    } else {
      Some(headers)
    }
  }

  def optional(body: String): Option[JValue] = {
    if(body == null || body.trim().size == 0) {
      None
    } else {
      Some(parse(body))
    }
  }

  def optional(body: JValue): Option[JValue] = {
    if(body == null) {
      None
    } else {
      Some(body)
    }
  }
}

object Request extends Optionals {
  def apply(method: String, path: String, headers: Map[String, String], body: String): Request = {
    Request(method, path, optional(headers), optional(body))
  }

  def apply(method: String, path: String, headers: Map[String, String], body: JValue): Request = {
    Request(method, path, optional(headers), optional(body))
  }

  def apply(method: String, path: String, headers: java.util.Map[String,String], body: String): Request = {
    Request(method, path, optional(JavaConversions.mapAsScalaMap(headers).toMap), optional(body))
  }
}

//TODO: support duplicate headers
case class Response(status: Int,
                    headers: Option[Map[String, String]],
                    body: Option[JValue]) {
  def bodyString:Option[String] = body.map{ b => compact(render(b)) }

  override def toString: String = {
    s"\tstatus: $status \n\theaders: $headers \n\tbody: \n${body.map{b => pretty(render(b))}}"
  }
}

object Response extends Optionals {
  def apply(status: Int, headers: Map[String, String], body: String): Response = {
    Response(status, optional(headers), optional(body))
  }

  def apply(status: Int, headers: Map[String, String], body: JValue): Response = {
    Response(status, optional(headers), optional(body))
  }

  def apply(status: Int, headers: java.util.Map[String, String], body: String): Response = {
    Response(status, optional(JavaConversions.mapAsScalaMap(headers).toMap), optional(body))
  }

  def invalidRequest(request: Request, pact: Pact) = {
    Response(500, Map[String, String](), "error"-> s"unexpected request : $request \nnot in : $pact")
  }
}


