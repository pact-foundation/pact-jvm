package au.com.dius.pact.model

import java.io.{InputStream, PrintWriter}
import java.util.jar.JarInputStream
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization

trait PactSerializer extends StrictLogging {
  this: Pact =>

  import org.json4s.JsonDSL._

  implicit def matcher2json(m: Matcher): JValue = {
    JObject(
      "expression" -> m.expression,
      "value" -> m.value
    )
  }

  implicit def request2json(r: Request): JValue = {
    JObject(
      "method" -> r.method,
      "path" -> r.path,
      "headers" -> r.headers,
      "query" -> r.query,
      "body" -> parseBody(r),
      "requestMatchingRules" -> r.matchers
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
      "responseMatchingRules" -> r.matchers
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

  implicit def pact2json(p: Pact): JValue = {
    JObject(
      "provider" -> provider,
      "consumer" -> consumer,
      "interactions" -> interactions,
      "metadata" -> Map( "pact-specification" -> Map("version" -> "2.0.0"), "pact-jvm" -> Map("version" -> lookupVersion))
    )
  }

  def serialize(writer: PrintWriter) {
    writer.print(pretty(render(this)))
  }

  def lookupVersion() = {
    val url = getClass.getProtectionDomain.getCodeSource.getLocation
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
  }
}
