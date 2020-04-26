package au.com.dius.pact.model.unfiltered

import java.net.URI
import java.util.zip.GZIPInputStream

import au.com.dius.pact.model.{OptionalBody, Request, Response}
import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.{HttpResponse => NHttpResponse}
import unfiltered.netty.ReceivedMessage
import unfiltered.request.HttpRequest
import unfiltered.response._

import scala.collection.JavaConversions
import scala.collection.JavaConverters._

@Deprecated
object Conversions extends StrictLogging {

  case class Headers(headers: java.util.Map[String, java.util.List[String]]) extends unfiltered.response.Responder[Any] {
    def respond(res: HttpResponse[Any]) {
      if (headers != null) {
        headers.asScala.foreach { case (key, value) => res.header(key, value.asScala.mkString(", ")) }
      }
    }
  }

  implicit def pactToUnfilteredResponse(response: Response): ResponseFunction[NHttpResponse] = {
    val headers = response.getHeaders
    if (response.getBody.isPresent) {
      Status(response.getStatus) ~> Headers(headers) ~> ResponseString(response.getBody.valueAsString)
    } else Status(response.getStatus) ~> Headers(headers)
  }

  def toHeaders(request: HttpRequest[ReceivedMessage]): java.util.Map[String, java.util.List[String]] = {
    request.headerNames.map(name => name -> request.headers(name).toList.asJava).toMap.asJava
  }

  def toQuery(request: HttpRequest[ReceivedMessage]): java.util.Map[String, java.util.List[String]] = {
    JavaConversions.mapAsJavaMap(request.parameterNames.map(name =>
      name -> JavaConversions.seqAsJavaList(request.parameterValues(name))).toMap)
  }

  def toPath(uri: String) = new URI(uri).getPath

  def toBody(request: HttpRequest[ReceivedMessage], charset: String = "UTF-8") = {
    val gzip = request.headers(ContentEncoding.GZip.name)
    val is = if (gzip.hasNext && gzip.next().contains("gzip")) {
      new GZIPInputStream(request.inputStream)
    } else {
      request.inputStream
    }
    if(is == null) "" else scala.io.Source.fromInputStream(is).mkString
  }

  def unfilteredRequestToPactRequest(request: HttpRequest[ReceivedMessage]): Request = {
    val headers = toHeaders(request)
    val contentTypeHeader = request.headers("Content-Type")
    val contentType = if (contentTypeHeader != null && contentTypeHeader.hasNext) new au.com.dius.pact.model.ContentType(contentTypeHeader.next())
      else au.com.dius.pact.model.ContentType.getTEXT_PLAIN
    new Request(request.method, toPath(request.uri), toQuery(request), headers,
      OptionalBody.body(toBody(request).getBytes(contentType.asCharset)))
  }
}
