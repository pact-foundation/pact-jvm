package au.com.dius.pact.pactbroker

import com.github.kittinunf.result.Result
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJson

data class PactResponse(val pactFile: Any, val links: Map<String, String>)

abstract class PactBrokerClientBase(val pactBrokerUrl: String, val options: Map<String, Any> = mapOf()) {

  protected abstract fun newHalClient(): IHalClient

  open fun publishVerificationResults(docAttributes: Map<String, Any>, result: Boolean, version: String,
                                      buildUrl: String? = null): Result<Boolean, Exception> {
    val halClient = newHalClient()
    val publishLink = docAttributes["pb:publish-verification-results"]
    return if (publishLink != null) {
      val jsonObject = jsonObject("success" to result, "providerApplicationVersion" to version)
      if (buildUrl != null) {
        jsonObject.add("buildUrl", buildUrl.toJson())
      }
      if (publishLink is Map<*, *> && publishLink.containsKey("href")) {
        halClient.postJson(publishLink["href"].toString(), jsonObject.toString())
      } else {
        Result.Failure(RuntimeException("Unable to publish verification results as there is no " +
          "pb:publish-verification-results link"))
      }
    } else {
      Result.Failure(RuntimeException("Unable to publish verification results as there is no " +
        "pb:publish-verification-results link"))
    }
  }
}
