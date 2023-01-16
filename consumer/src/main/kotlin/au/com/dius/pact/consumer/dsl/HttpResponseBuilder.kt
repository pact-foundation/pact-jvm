package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.HttpResponse

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
   * Sets the status code of the response
   */
  fun status(status: Int): HttpResponseBuilder {
    response.status = status
    return this
  }

  override fun body(body: String): HttpResponseBuilder {
    return super.body(body) as HttpResponseBuilder
  }

  override fun body(body: String, contentTypeString: String?): HttpResponseBuilder {
    return super.body(body, contentTypeString) as HttpResponseBuilder
  }
}
