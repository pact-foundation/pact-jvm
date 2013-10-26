package com.dius.pact.runner

import play.api.libs.ws.Response
import play.api.libs.ws.WS

object HttpCalls {
  implicit def wrapResponse(response: Response):WrappedResponse = WrappedResponse(response)

  case class WrappedResponse(response: Response) {
    /**
     * Get all response headers.
     */
    def allHeaders: Map[String, Seq[String]] = {
      import scala.collection.JavaConverters._
      println(s"response=$response")
      println(s"ahcresponse=${response.getAHCResponse}")
      mapAsScalaMapConverter(response.getAHCResponse.getHeaders).asScala.map(e => e._1 -> e._2.asScala.toSeq).toMap
    }
  }
}

trait HttpCalls {
  def url(remoteUrl:String) = WS.url(remoteUrl)
}
