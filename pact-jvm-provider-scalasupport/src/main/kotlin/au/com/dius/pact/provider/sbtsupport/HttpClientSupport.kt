package au.com.dius.pact.provider.sbtsupport

import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.Response
import org.apache.commons.lang3.StringUtils
import org.asynchttpclient.DefaultAsyncHttpClient
import org.asynchttpclient.RequestBuilder
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture

object HttpClientSupport {

  fun executeRequest(request: RequestBuilder): CompletableFuture<Response> {
    val asyncHttpClient = DefaultAsyncHttpClient()
    return asyncHttpClient.executeRequest(request).toCompletableFuture().thenApply({ res ->
      val headers = mutableMapOf<String, String>()
      res.headers.names().forEach({ name -> headers.put(name, res.getHeader(name)) })
      val contentType = if (StringUtils.isEmpty(res.contentType)) {
        org.apache.http.entity.ContentType.APPLICATION_JSON
      } else {
        org.apache.http.entity.ContentType.parse(res.contentType)
      }
      val charset = if (contentType.charset == null) Charset.forName("UTF-8") else contentType.charset
      val body = if (res.hasResponseBody()) {
        OptionalBody.body(res.getResponseBody(charset))
      } else {
        OptionalBody.empty()
      }
      Response(res.statusCode, headers, body)
    })
  }
}
