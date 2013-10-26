package com.dius.pact.runner

import play.api.libs.ws.Response
import play.api.libs.ws.WS
import play.api.libs.ws.WS.WSRequestHolder
import scala.concurrent.Future
import play.api.libs.json.JsValue

object HttpCalls {
  implicit def wrapResponse(response: Response):WrappedResponse = WrappedResponse(response)

  case class WrappedResponse(response: Response) {
    /**
     * Get all response headers.
     */
    def allHeaders: Map[String, Seq[String]] = {
      import scala.collection.JavaConverters._
      mapAsScalaMapConverter(response.getAHCResponse.getHeaders).asScala.map(e => e._1 -> e._2.asScala.toSeq).toMap
    }
  }
}

trait HttpCalls {
  def url(remoteUrl:String) = WS.url(remoteUrl)

  def post(request:WSRequestHolder, body:String):Future[Response] = {
    request.post(body)
  }

  def post(request:WSRequestHolder, body:JsValue):Future[Response] = {
    request.post(body)
  }

  def get(request:WSRequestHolder):Future[Response] = {
    request.get()
  }

  def put(request:WSRequestHolder, body:String):Future[Response] = {
    request.put(body)
  }
}
