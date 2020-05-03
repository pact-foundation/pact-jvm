package au.com.dius.pact.core.pactbroker

import au.com.dius.pact.core.support.Json
import com.google.gson.JsonElement

data class PactResult(
  val name: String,
  val source: String,
  val pactBrokerUrl: String,
  val pactFileAuthentication: List<String> = listOf(),
  val notices: List<VerificationNotice> = listOf()
)

data class VerificationNotice(
  val `when`: String,
  val text: String
) {
  companion object {
    fun fromJson(json: JsonElement): VerificationNotice {
      val jsonObj = json.asJsonObject
      return VerificationNotice(Json.toString(jsonObj["when"]), Json.toString(jsonObj["text"]))
    }
  }
}
