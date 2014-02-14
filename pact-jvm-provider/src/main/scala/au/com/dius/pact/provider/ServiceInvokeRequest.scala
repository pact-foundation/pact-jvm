package au.com.dius.pact.provider

import au.com.dius.pact.model.Request
import au.com.dius.pact.model.finagle.Conversions._
import org.jboss.netty.handler.codec.http._

object ServiceInvokeRequest {
  def apply(url: String, request: Request):HttpRequest = {
    request.copy(path = s"$url${request.path}")
  }
}