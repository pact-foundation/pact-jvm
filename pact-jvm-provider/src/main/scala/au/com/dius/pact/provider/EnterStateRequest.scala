package au.com.dius.pact.provider

import org.jboss.netty.handler.codec.http.HttpRequest
import com.twitter.finagle.http.RequestBuilder
import au.com.dius.pact.model.finagle.Conversions._
import org.json4s.JValue

object EnterStateRequest {
  def apply(url: String, state: String): HttpRequest = {
    import org.json4s.JsonDSL._
    val json: JValue = "state" -> state
    println(s"posting state $state to url $url")
    RequestBuilder()
      .url(url)
      .buildPost(json)
  }
}
