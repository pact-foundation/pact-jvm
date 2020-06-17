package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.Category
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.support.property
import groovy.json.JsonBuilder
import mu.KLogging

open class BaseBuilder : Matchers() {
  protected fun setupBody(requestData: Map<String, Any>, request: MutableMap<String, Any>) {
    if (requestData.containsKey(BODY)) {
      val body = requestData[BODY]
      if (body != null && body::class.qualifiedName == "au.com.dius.pact.consumer.groovy.PactBodyBuilder") {
        request[BODY] = body::class.property(BODY)?.get(body) as Any
        val matchers = request["matchers"] as MatchingRules
        matchers.addCategory(body::class.property("matchers")?.get(body) as Category)
        val generators = request["generators"] as Generators
        generators.addGenerators(body::class.property("generators")?.get(body) as Generators)
      } else if (body != null && body !is String) {
        val prettyPrint = requestData["prettyPrint"] as Boolean?
        if (prettyPrint == null && !compactMimeTypes(requestData) || prettyPrint == true) {
          request[BODY] = JsonBuilder(body).toPrettyString()
        } else {
          request[BODY] = JsonBuilder(body).toString()
        }
      }
    }
  }

  companion object : KLogging() {
    const val CONTENT_TYPE = "Content-Type"
    const val JSON = "application/json"
    const val BODY = "body"
    const val LOCALHOST = "localhost"
    const val HEADER = "header"

    val COMPACT_MIME_TYPES = listOf("application/x-thrift+json")

    @JvmStatic
    fun compactMimeTypes(reqResData: Map<String, Any>): Boolean {
      return if (reqResData.containsKey("headers")) {
        val headers = reqResData["headers"] as Map<String, String>
        headers.entries.find { it.key == CONTENT_TYPE }?.value in COMPACT_MIME_TYPES
      } else false
    }

    @JvmStatic
    fun isCompactMimeType(mimetype: String) = mimetype in COMPACT_MIME_TYPES
  }
}
