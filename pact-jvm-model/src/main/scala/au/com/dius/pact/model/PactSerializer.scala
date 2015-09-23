package au.com.dius.pact.model

import java.io.{InputStream, PrintWriter}
import java.util.jar.JarInputStream

import com.typesafe.scalalogging.StrictLogging
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization

object PactSerializer extends StrictLogging {

  import org.json4s.JsonDSL._

  def valueToJson(value: Any) : JValue = {
    implicit val formats = Serialization.formats(NoTypeHints)
    value match {
      case map: Map[String, Any] => JObject(map.mapValues(v => valueToJson(v)).toList.map(entry => JField(entry._1, entry._2)))
      case _ => Extraction.decompose(value)
    }
  }

  def matchers2json(m: Option[Map[String, Any]]): JValue = {
    m match {
        case None => JNothing
        case Some(v) => valueToJson(v)
    }
  }

  implicit def request2json(r: Request): JValue = {
    JObject(
      "method" -> r.method.toUpperCase,
      "path" -> r.path,
      "headers" -> r.headers,
      "query" -> r.query,
      "body" -> parseBody(r),
      "matchingRules" -> matchers2json(r.matchers)
    )
  }

  def parseBody(r: HttpPart) = {
    r.body match {
      case None => JNothing
      case Some(s) => if (r.jsonBody) parse(s) else JString(s)
    }
  }

  implicit def response2json(r: Response): JValue = {
    JObject(
      "status" -> JInt(r.status),
      "headers" -> r.headers,
      "body" -> parseBody(r),
      "matchingRules" -> matchers2json(r.matchers)
    )
  }

  implicit def interaction2json(i: Interaction): JValue = {
    JObject (
      "providerState" -> i.providerState,
      "description" -> i.description,
      "request" -> i.request,
      "response" -> i.response
    )
  }

  implicit def provider2json(p: Provider): JValue = {
    Map("name" -> p.name)
  }

  implicit def consumer2json(c: Consumer): JValue = {
    Map("name" -> c.name)
  }

  def pact2jsonV2(p: Pact): JValue = {
    JObject(
      "provider" -> p.provider,
      "consumer" -> p.consumer,
      "interactions" -> p.interactions,
      "metadata" -> Map("pact-specification" -> Map("version" -> "2.0.0"), "pact-jvm" -> Map("version" -> lookupVersion))
    )
  }

  def pact2jsonV3(p: Pact): JValue = {
    JObject(
      "provider" -> p.provider,
      "consumer" -> p.consumer,
      "interactions" -> p.interactions,
      "metadata" -> Map("pact-specification" -> Map("version" -> "3.0.0"), "pact-jvm" -> Map("version" -> lookupVersion))
    )
  }

  def renderV2(p: Pact): JValue = render(pact2jsonV2(p))

  def renderV3(p: Pact): JValue = render(pact2jsonV3(p))

  def serialize(p: Pact, writer: PrintWriter, config: PactConfig = PactConfig(2)) {
    config.pactVersion match {
      case 3 => writer.print(pretty(renderV3(p)))
      case _ => writer.print(pretty(renderV2(p)))
    }
  }

  def from(source: String): Pact = {
    from(parse(StringInput(source)))
  }

  def from(source: JsonInput): Pact = {
    from(parse(source))
  }

  def from(json:JValue) = {
    implicit val formats = DefaultFormats
    val transformedJson = json.transformField {
      case ("provider_state", value) => ("providerState", value)
      case ("responseMatchingRules", value) => ("matchingRules", value)
      case ("requestMatchingRules", value) => ("matchingRules", value)
      case ("method", value) => ("method", JString(value.values.toString.toUpperCase))
    }
    val provider = (transformedJson \ "provider").extract[Provider]
    val consumer = (transformedJson \ "consumer").extract[Consumer]

    val interactions = (transformedJson \ "interactions").children.map(i => {
      val interaction = i.extract[Interaction]
      val requestBody = extractBody(i \ "request" \ "body")
      val request = (i \ "request").extract[Request].copy(body = requestBody)
      val responseBody = extractBody(i \ "response" \ "body")
      val response = (i \ "response").extract[Response].copy(body = responseBody)
      interaction.copy(request = request, response = response)
    })
    Pact(provider, consumer, interactions)
  }

  def extractBody(body: JValue): Option[String] = {
    body match {
      case JString(s) => Some(s)
      case JNothing => None
      case JNull => None
      case b => Some(compact(b))
    }
  }

  def lookupVersion() = {
    val url = getClass.getProtectionDomain.getCodeSource.getLocation
    if (url != null) {
      val openStream: InputStream = url.openStream()
      try {
        val jarStream = new JarInputStream(openStream)
        val manifest = jarStream.getManifest
        val attributes = manifest.getMainAttributes
        attributes.getValue("Implementation-Version")
      }
      catch {
        case e : Throwable => logger.warn("Could not load pact-jvm manifest", e); ""
      }
      finally openStream.close()
    } else ""
  }
}
