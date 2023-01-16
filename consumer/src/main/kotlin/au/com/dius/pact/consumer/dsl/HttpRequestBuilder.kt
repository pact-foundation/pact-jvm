package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.HttpRequest

/**
 * Pact HTTP Request builder DSL that supports V4 formatted Pact files
 */
open class HttpRequestBuilder(private val request: HttpRequest): HttpPartBuilder(request) {
  /**
   * Terminate the builder and return the request object
   */
  fun build(): HttpRequest {
    return request
  }

  /**
   * HTTP Method for the request.
   */
  fun method(method: String): HttpRequestBuilder {
    request.method = method
    return this
  }

  /**
   * Path for the request.
   */
  fun path(path: String): HttpRequestBuilder {
    request.path = path
    return this
  }

  override fun header(key: String, value: Any): HttpRequestBuilder {
    return super.header(key, value) as HttpRequestBuilder
  }

  override fun headers(key: String, value: String, vararg nameValuePairs: String): HttpRequestBuilder {
    return super.headers(key, value, nameValuePairs) as HttpRequestBuilder
  }

  override fun headers(vararg nameValuePairs: Pair<String, String>): HttpRequestBuilder {
    return super.headers(nameValuePairs) as HttpRequestBuilder
  }

  override fun headers(values: Map<String, Any>): HttpRequestBuilder {
    return super.headers(values) as HttpRequestBuilder
  }

  override fun body(body: String): HttpRequestBuilder {
    return super.body(body) as HttpRequestBuilder
  }

  override fun body(body: String, contentTypeString: String?): HttpRequestBuilder {
    return super.body(body, contentTypeString) as HttpRequestBuilder
  }
}
