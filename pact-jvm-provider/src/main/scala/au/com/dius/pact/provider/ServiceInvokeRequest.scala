package au.com.dius.pact.provider

import au.com.dius.pact.model.Request

object ServiceInvokeRequest {
  def apply(url: String, request: Request):Request = {
    request.copy(path = s"$url${request.path}")
  }
}