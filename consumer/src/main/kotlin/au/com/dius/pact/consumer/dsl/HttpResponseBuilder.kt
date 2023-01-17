package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.HttpResponse
import au.com.dius.pact.core.model.matchingrules.HttpStatus
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.RuleLogic
import au.com.dius.pact.core.model.matchingrules.StatusCodeMatcher
import java.util.regex.Pattern

/**
 * Pact HTTP Response builder DSL that supports V4 formatted Pact files
 */
open class HttpResponseBuilder(private val response: HttpResponse): HttpPartBuilder(response) {
  /**
   * Terminate the builder and return the response object
   */
  fun build(): HttpResponse {
    return response
  }

  override fun header(key: String, value: Any): HttpResponseBuilder {
    return super.header(key, value) as HttpResponseBuilder
  }

  override fun headers(key: String, value: String, vararg nameValuePairs: String): HttpResponseBuilder {
    return super.headers(key, value, nameValuePairs) as HttpResponseBuilder
  }

  override fun headers(vararg nameValuePairs: Pair<String, Any>): HttpResponseBuilder {
    return super.headers(nameValuePairs) as HttpResponseBuilder
  }

  override fun headers(values: Map<String, Any>): HttpResponseBuilder {
    return super.headers(values) as HttpResponseBuilder
  }

   /**
     * Match a set cookie header
     * @param cookie Cookie name to match
     * @param regex Regex to match the cookie value with
     * @param example Example value
     */
   fun matchSetCookie(cookie: String, regex: String, example: String): HttpResponseBuilder {
      val headerCategory = response.matchingRules.addCategory("header")
      if (headerCategory.numRules("set-cookie") > 0) {
        headerCategory.addRule("set-cookie", RegexMatcher(Pattern.quote("$cookie=") + regex))
      } else {
        headerCategory.setRule("set-cookie", RegexMatcher(Pattern.quote("$cookie=") + regex), RuleLogic.OR)
      }
      if (response.headers.containsKey("set-cookie")) {
        response.headers["set-cookie"] = response.headers["set-cookie"]!!.plus("$cookie=$example")
      } else {
        response.headers["set-cookie"] = listOf("$cookie=$example")
      }
      return this
   }

  /**
   * Sets the status code of the response
   */
  fun status(status: Int): HttpResponseBuilder {
    response.status = status
    return this
  }

  /**
   * Match any HTTP Information response status (100-199)
   */
  fun informationStatus(): HttpResponseBuilder {
    response.matchingRules.addCategory("status").addRule(StatusCodeMatcher(HttpStatus.Information))
    response.status = 100
    return this
  }

  /**
   * Match any HTTP success response status (200-299)
   */
  fun successStatus(): HttpResponseBuilder {
    val matcher = StatusCodeMatcher(HttpStatus.Success)
    response.matchingRules.addCategory("status").addRule(matcher)
    response.status = 200
    return this
  }

  /**
   * Match any HTTP redirect response status (300-399)
   */
  fun redirectStatus(): HttpResponseBuilder {
    val matcher = StatusCodeMatcher(HttpStatus.Redirect)
    response.matchingRules.addCategory("status").addRule(matcher)
    response.status = 300
    return this
  }

  /**
   * Match any HTTP client error response status (400-499)
   */
  fun clientErrorStatus(): HttpResponseBuilder {
    val matcher = StatusCodeMatcher(HttpStatus.ClientError)
    response.matchingRules.addCategory("status").addRule(matcher)
    response.status = 400
    return this
  }

  /**
   * Match any HTTP server error response status (500-599)
   */
  fun serverErrorStatus(): HttpResponseBuilder {
    val matcher = StatusCodeMatcher(HttpStatus.ServerError)
    response.matchingRules.addCategory("status").addRule(matcher)
    response.status = 500
    return this
  }

  /**
   * Match any HTTP non-error response status (< 400)
   */
  fun nonErrorStatus(): HttpResponseBuilder {
    val matcher = StatusCodeMatcher(HttpStatus.NonError)
    response.matchingRules.addCategory("status").addRule(matcher)
    response.status = 200
    return this
  }

  /**
   * Match any HTTP error response status (>= 400)
   */
  fun errorStatus(): HttpResponseBuilder {
    val matcher = StatusCodeMatcher(HttpStatus.Error)
    response.matchingRules.addCategory("status").addRule(matcher)
    response.status = 400
    return this
  }

  /**
   * Match any HTTP status code in the provided list
   */
  fun statusCodes(statusCodes: List<Int>): HttpResponseBuilder {
    val matcher = StatusCodeMatcher(HttpStatus.StatusCodes, statusCodes)
    response.matchingRules.addCategory("status").addRule(matcher)
    response.status = statusCodes.first()
    return this
  }

  override fun body(body: String): HttpResponseBuilder {
    return super.body(body) as HttpResponseBuilder
  }

  override fun body(body: String, contentTypeString: String?): HttpResponseBuilder {
    return super.body(body, contentTypeString) as HttpResponseBuilder
  }

  override fun body(dslPart: DslPart): HttpResponseBuilder {
    return super.body(dslPart) as HttpResponseBuilder
  }

  override fun body(builder: BodyBuilder): HttpResponseBuilder {
    return super.body(builder) as HttpResponseBuilder
  }

  override fun bodyMatchingContentType(contentType: String, exampleContents: ByteArray): HttpResponseBuilder {
    return super.bodyMatchingContentType(contentType, exampleContents) as HttpResponseBuilder
  }

  override fun bodyMatchingContentType(contentType: String, exampleContents: String): HttpResponseBuilder {
    return super.bodyMatchingContentType(contentType, exampleContents) as HttpResponseBuilder
  }
}
