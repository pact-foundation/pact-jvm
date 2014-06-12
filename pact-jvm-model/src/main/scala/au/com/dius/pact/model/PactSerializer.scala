package au.com.dius.pact.model

import java.io.{InputStream, File, PrintWriter}
import java.util.jar.JarInputStream
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.json4s._
import org.json4s.jackson.JsonMethods._


trait PactSerializer extends StrictLogging {
  this: Pact =>

  import org.json4s.JsonDSL._

  implicit def request2json(r: Request): JValue = {
    JObject(
      "method" -> r.method,
      "path" -> r.path,
      "headers" -> r.headers,
      "body" -> r.body
    )
  }

  implicit def response2json(r: Response): JValue = {
    JObject(
      "status" -> JInt(r.status),
      "headers" -> r.headers,
      "body" -> r.body
    )
  }

  implicit def interaction2json(i: Interaction): JValue = {
    JObject (
      "provider_state" -> i.providerState,
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
      "metadata" -> Map( "pact_gem" -> Map("version" -> "1.0.9"), "pact-jvm" -> Map("version" -> lookupVersion))
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
