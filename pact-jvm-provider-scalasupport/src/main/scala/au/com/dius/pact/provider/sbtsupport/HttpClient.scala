package au.com.dius.pact.provider.sbtsupport

import java.nio.charset.Charset
import java.util

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.model.{Request, Response}
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.lang3.StringUtils
import org.asynchttpclient.{DefaultAsyncHttpClient, RequestBuilder}

import scala.compat.java8.FutureConverters
import scala.concurrent.Future

object HttpClient extends StrictLogging {

  def run(request: Request): Future[Response] = {
    logger.debug("request=" + request)
    val req = new RequestBuilder(request.getMethod)
      .setUrl(request.getPath)
      .setQueryParams(request.getQuery)
    if (request.getHeaders != null) {
      request.getHeaders.forEach((name, value) => req.addHeader(name, value))
    }
    if (request.getBody.isPresent) {
      req.setBody(request.getBody.getValue)
    }

    val asyncHttpClient = new DefaultAsyncHttpClient
    FutureConverters.toScala[Response](asyncHttpClient.executeRequest(req).toCompletableFuture.thenApply(res => {
      val headers = new util.HashMap[String, String]()
      res.getHeaders.names().forEach(name => headers.put(name, res.getHeader(name)))
      val contentType = if (StringUtils.isEmpty(res.getContentType))
        org.apache.http.entity.ContentType.APPLICATION_JSON
      else
        org.apache.http.entity.ContentType.parse(res.getContentType)
      val charset = if (contentType.getCharset == null) Charset.forName("UTF-8") else contentType.getCharset
      val body = if (res.hasResponseBody) {
        OptionalBody.body(res.getResponseBody(charset))
      } else {
        OptionalBody.empty()
      }
      new Response(res.getStatusCode, headers, body)
    }))
  }
}
