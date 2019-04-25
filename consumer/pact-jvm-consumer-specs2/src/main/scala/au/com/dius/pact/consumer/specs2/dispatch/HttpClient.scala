package au.com.dius.pact.consumer.specs2.dispatch

import java.nio.charset.Charset
import java.util
import java.util.concurrent.CompletableFuture

import au.com.dius.pact.core.model.{OptionalBody, Response}
import au.com.dius.pact.core.model.Request
import org.apache.commons.lang3.StringUtils
import org.asynchttpclient.{DefaultAsyncHttpClient, RequestBuilder}
import scala.collection.JavaConverters._

object HttpClient {

  def run(request: Request): CompletableFuture[Response] = {
    val req = new RequestBuilder(request.getMethod)
      .setUrl(request.getPath)
      .setQueryParams(request.getQuery)
    request.getHeaders.forEach((name, value) => req.addHeader(name, value.asScala.mkString(", ")))
    if (request.getBody.isPresent) {
      req.setBody(request.getBody.getValue)
    }

    val asyncHttpClient = new DefaultAsyncHttpClient
    asyncHttpClient.executeRequest(req).toCompletableFuture.thenApply(res => {
      val headers = new util.HashMap[String, util.List[String]]()
      res.getHeaders.names().forEach(name => headers.put(name, List(res.getHeader(name)).asJava))
      val contentType = if (StringUtils.isEmpty(res.getContentType))
        org.apache.http.entity.ContentType.APPLICATION_JSON
      else
        org.apache.http.entity.ContentType.parse(res.getContentType)
      val charset = if (contentType.getCharset == null) Charset.forName("UTF-8") else contentType.getCharset
      val body = if (res.hasResponseBody) {
        OptionalBody.body(res.getResponseBody(charset).getBytes)
      } else {
        OptionalBody.empty()
      }
      new Response(res.getStatusCode, headers, body)
    })
  }
}
