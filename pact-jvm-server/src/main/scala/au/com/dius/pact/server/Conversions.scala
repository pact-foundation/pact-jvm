package au.com.dius.pact.server

import java.net.URI
import java.util.zip.GZIPInputStream

import au.com.dius.pact.core.model.{OptionalBody, ContentType, Request, Response}
import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.{HttpResponse => NHttpResponse}
import unfiltered.netty.ReceivedMessage
import unfiltered.request.HttpRequest
import unfiltered.response.{ContentEncoding, HttpResponse, ResponseFunction, ResponseString, Status}

import scala.collection.JavaConverters._

object Conversions extends StrictLogging {

  case class Headers(headers: java.util.Map[String, java.util.List[String]]) extends unfiltered.response.Responder[Any] {
    def respond(res: HttpResponse[Any]) {
      if (headers != null) {
        headers.asScala.foreach { case (key, value) => res.header(key, value.asScala.mkString(", ")) }
      }
    }
  }

  def pactToUnfilteredResponse(response: Response): ResponseFunction[NHttpResponse] = {
    val headers = response.getHeaders
    if (response.getBody.isPresent) {
      Status(response.getStatus) ~> Headers(headers) ~> ResponseString(response.getBody.valueAsString)
    } else Status(response.getStatus) ~> Headers(headers)
  }

  def toHeaders(request: HttpRequest[ReceivedMessage]): java.util.Map[String, java.util.List[String]] = {
    request.headerNames.map(name => name -> request.headers(name).toList.asJava).toMap.asJava
  }

  def toQuery(request: HttpRequest[ReceivedMessage]): java.util.Map[String, java.util.List[String]] = {
    request.parameterNames.map(name => name -> request.parameterValues(name).asJava).toMap.asJava
  }

  def toPath(uri: String) = new URI(uri).getPath

  private def toBodyInputStream(request: HttpRequest[ReceivedMessage]) = {
    val gzip = request.headers(ContentEncoding.GZip.name)
    if (gzip.hasNext && gzip.next().contains("gzip")) {
      new GZIPInputStream(request.inputStream)
    } else {
      request.inputStream
    }
  }

  private def toBody(request: HttpRequest[ReceivedMessage], contentType: ContentType) = {
    val inputStream = toBodyInputStream(request)
    if (inputStream == null)
      OptionalBody.empty()
    else
      OptionalBody.body(org.apache.commons.io.IOUtils.toByteArray(inputStream), contentType)
  }

  def unfilteredRequestToPactRequest(request: HttpRequest[ReceivedMessage]): Request = {
    val headers = toHeaders(request)
    val contentTypeHeader = request.headers("Content-Type")
    val contentType = if (contentTypeHeader.hasNext) new ContentType(contentTypeHeader.next())
      else ContentType.getTEXT_PLAIN
    new Request(request.method, toPath(request.uri), toQuery(request), headers, toBody(request, contentType))
  }
}
