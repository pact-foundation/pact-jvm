package au.com.dius.pact.provider.unfiltered

import java.io.{BufferedReader, InputStreamReader}
import java.net.URI
import java.util.zip.GZIPInputStream

import au.com.dius.pact.model.{OptionalBody, Request, Response}
import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.{HttpResponse => NHttpResponse}
import unfiltered.netty.ReceivedMessage
import unfiltered.request.HttpRequest
import unfiltered.response._

import scala.collection.JavaConversions
import scala.collection.immutable.Stream

object Conversions extends StrictLogging {

  case class Headers(headers: java.util.Map[String, String]) extends unfiltered.response.Responder[Any] {
    def respond(res: HttpResponse[Any]) {
      import collection.JavaConversions._
      if (headers != null) {
        headers.foreach { case (key, value) => res.header(key, value) }
      }
    }
  }

  implicit def pactToUnfilteredResponse(response: Response): ResponseFunction[NHttpResponse] = {
    if (response.getBody.isPresent) {
      Status(response.getStatus) ~> Headers(response.getHeaders) ~> ResponseString(response.getBody.valueAsString())
    } else Status(response.getStatus) ~> Headers(response.getHeaders)
  }

  def toHeaders(request: HttpRequest[ReceivedMessage]): java.util.Map[String, String] = {
    JavaConversions.mapAsJavaMap(request.headerNames.map(name =>
      name -> request.headers(name).mkString(",")).toMap)
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
    new Request(request.method, toPath(request.uri), toQuery(request), toHeaders(request),
      OptionalBody.body(toBody(request).getBytes))
  }
}
