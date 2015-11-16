package au.com.dius.pact.provider

import au.com.dius.pact.model.Request

object EnterStateRequest {
  def apply(url: String, state: String): Request = {
    new Request("POST", url, null, null, "{\"state\": \"" + state + "\"}", null)
  }
}
