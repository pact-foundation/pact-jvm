package au.com.dius.pact.consumer.specs2

import au.com.dius.pact.consumer.dispatch.HttpClient
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.PactReader
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.Response

object TestHttpSupport {

  fun extractFrom(body: OptionalBody) = body.orElse("") == "{\"responsetest\": true}"

  fun extractResponseTest(serverUrl: String, path: String, request: Request): Boolean {
    val r = request.copy()
    r.path = "$serverUrl$path"
    return HttpClient.run(r).thenApply({ response: Response -> response.status == 200 && extractFrom(response.body) }).get()
  }

  fun simpleGet(serverUrl: String, path: String) =
    HttpClient.run(Request("GET", serverUrl + path))
      .thenApply({ response -> response.status to response.body.value }).get()

  fun simpleGet(serverUrl: String, path: String, query: String) =
    HttpClient.run(Request("GET", serverUrl + path, PactReader.queryStringToMap(query, true)))
      .thenApply({ response -> response.status to response.body.value }).get()

  fun options(serverUrl: String, path: String) =
    HttpClient.run(Request("OPTION", serverUrl + path))
      .thenApply({ response -> Triple(response.status, response.body.orElse(""), response.headers) }).get()
}
