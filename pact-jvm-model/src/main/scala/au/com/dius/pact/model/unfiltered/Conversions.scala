package au.com.dius.pact.model.unfiltered

import au.com.dius.pact.model.{Response, Request}
import unfiltered.request.HttpRequest
import scala.io.Source
import unfiltered.netty.ReceivedMessage
import unfiltered.response.{ResponseString, ResponseFunction, HttpResponse, Status}
import org.jboss.netty.handler.codec.http.{HttpResponse => NHttpResponse}
import com.ning.http.client
import com.ning.http.client.FluentCaseInsensitiveStringsMap

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
    Some(request.parameterNames.map(name => request.parameterValues(name).map(name + "=" + _)).flatten.mkString("&"))
  }

  def toPath(uri: String) = {
    uri.split('?').head
  }

  implicit def unfilteredRequestToPactRequest(request: HttpRequest[ReceivedMessage]): Request = {
    Request(request.method, toPath(request.uri), toQuery(request), toHeaders(request),
      Some(Source.fromInputStream(request.inputStream).mkString("")), None)
  }
}
