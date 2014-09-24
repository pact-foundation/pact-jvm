package au.com.dius.pact.model.unfiltered

import java.io.{InputStreamReader, BufferedReader, Reader}
import java.util.zip.GZIPInputStream

import au.com.dius.pact.model.{Response, Request}
import unfiltered.request.HttpRequest
import unfiltered.netty.ReceivedMessage
import unfiltered.response._
import io.netty.handler.codec.http.{HttpResponse => NHttpResponse}
import com.ning.http.client
import com.ning.http.client.FluentCaseInsensitiveStringsMap
import scala.collection.immutable.Stream

object Conversions {

  def toMap(map: FluentCaseInsensitiveStringsMap): Map[String, String] = {
    import collection.JavaConversions._
    map.entrySet().map(e => e.getKey -> e.getValue.mkString(",")).toMap
  }

  implicit def dispatchResponseToPactResponse(response: client.Response): Response = {
    Response(response.getStatusCode, Some(toMap(response.getHeaders)), Some(response.getResponseBody), None)
  }

  case class Headers(headers: Option[Map[String, String]]) extends unfiltered.response.Responder[Any] {
    def respond(res: HttpResponse[Any]) {
      headers.foreach(_.foreach { case (key, value) => res.header(key, value)})
    }
  }

  implicit def pactToUnfilteredResponse(response: Response): ResponseFunction[NHttpResponse] = {
    val rf = Status(response.status) ~> Headers(response.headers)
    response.body.fold(rf)(rf ~> ResponseString(_))
  }

  def toHeaders(request: HttpRequest[ReceivedMessage]): Option[Map[String, String]] = {
    Some(request.headerNames.map(name => name -> request.headers(name).mkString(",")).toMap)
  }

  def toQuery(request: HttpRequest[ReceivedMessage]): Option[String] = {
    val queryString = request.parameterNames.map(name => request.parameterValues(name).map(name + "=" + _)).flatten.mkString("&")
    if (queryString.isEmpty)
      None
    else
      Some(queryString)
  }

  def toPath(uri: String) = {
    uri.split('?').head
  }

  def toBody(request: HttpRequest[ReceivedMessage]) = {
    val br = if (request.headers(ContentEncoding.GZip.name).contains("gzip")) {
      new BufferedReader(new InputStreamReader(new GZIPInputStream(request.inputStream)))
    } else {
      new BufferedReader(request.reader)
    }
    val body = Stream.continually(br.readLine()).takeWhile(_ != null).mkString("\n")
    if (body.isEmpty)
      None
    else
      Some(body)
  }

  implicit def unfilteredRequestToPactRequest(request: HttpRequest[ReceivedMessage]): Request = {
    Request(request.method, toPath(request.uri), toQuery(request), toHeaders(request), toBody(request), None)
  }
}
