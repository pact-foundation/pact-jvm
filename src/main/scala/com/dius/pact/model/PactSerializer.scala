package com.dius.pact.model

import java.io.PrintWriter
import org.json4s._
import org.json4s.jackson.JsonMethods._


trait PactSerializer {
  this: Pact =>

  import org.json4s.JsonDSL._
  
  implicit def request2json(r: Request): JValue = {
    JObject(
      "method" -> r.method.toString,
      "headers" -> r.headers,
      "path" -> r.path
    )
  }
  
  implicit def response2json(r: Response): JValue = {
    JObject(
      "status" -> JInt(r.status),
      "headers" -> r.headers,
      "body" -> r.bodyJson
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
      "metadata" -> Map( "pact_gem" -> Map("version" -> "1.0.9"))
    )
  }

  def serialize(writer: PrintWriter) {
    writer.print(pretty(render(this)))
  }
}
