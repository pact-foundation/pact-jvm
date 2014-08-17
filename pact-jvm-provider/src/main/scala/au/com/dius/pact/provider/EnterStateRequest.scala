package au.com.dius.pact.provider

import org.json4s.JValue
import au.com.dius.pact.model.Request

object EnterStateRequest {
  def apply(url: String, state: String): Request = {
    import org.json4s.JsonDSL._
    val json: JValue = "state" -> state
    Request("POST", url, Map[String, Seq[String]](), Map[String, String](), json, null)
  }
}
