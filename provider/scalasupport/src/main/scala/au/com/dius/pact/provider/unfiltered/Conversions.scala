package au.com.dius.pact.provider.unfiltered

import java.io.{BufferedReader, InputStreamReader}
import java.net.URI
import java.util.zip.GZIPInputStream

import au.com.dius.pact.core.model.{OptionalBody, ContentType, Response}
import au.com.dius.pact.core.model.Request
import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.{HttpResponse => NHttpResponse}
import unfiltered.netty.ReceivedMessage
import unfiltered.request.HttpRequest
import unfiltered.response.{ContentEncoding, HttpResponse, ResponseFunction, ResponseString, Status}

import scala.collection.JavaConversions
import scala.collection.JavaConverters._
import scala.collection.immutable.Stream

object Conversions extends StrictLogging {

  case class Headers(headers: java.util.Map[String, java.util.List[String]]) extends unfiltered.response.Responder[Any] {
    def respond(res: HttpResponse[Any]) {
      if (headers != null) {
        headers.asScala.foreach { case (key, value) => res.header(key, value.asScala.mkString(", ")) }
      }
    }
  }

  implicit def pactToUnfilteredResponse(response: Response): ResponseFunction[NHttpResponse] = {
    if (response.getBody.isPresent) {
      Status(response.getStatus) ~> Headers(response.getHeaders) ~> ResponseString(response.getBody.valueAsString())
    } else Status(response.getStatus) ~> Headers(response.getHeaders)
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
    val br = if (request.headers(ContentEncoding.GZip.name).contains("gzip")) {
      new BufferedReader(new InputStreamReader(new GZIPInputStream(request.inputStream)))
    } else {
      new BufferedReader(request.reader)
    }
    Stream.continually(br.readLine()).takeWhile(_ != null).mkString("\n")
  }

  implicit def unfilteredRequestToPactRequest(request: HttpRequest[ReceivedMessage]): Request = {
    val contentType = new ContentType(request.headers("Content-Type").next())
    new Request(request.method, toPath(request.uri), toQuery(request), toHeaders(request),
      OptionalBody.body(toBody(request).getBytes(contentType.asCharset), contentType))
  }
}
