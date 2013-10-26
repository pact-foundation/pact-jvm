package util

import play.api.libs.ws.{Response => WsResponse}
import scala.concurrent.Future
import com.dius.pact.runner.HttpCalls
import org.specs2.mock.Mockito
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import com.ning.http.client.{Response => AhcResponse, FluentCaseInsensitiveStringsMap}
import com.dius.pact.model.{Response => PactResponse, Request}
import scala.Predef._
import play.api.libs.ws.WS.WSRequestHolder
import java.{util => ju}
import play.api.libs.json.JsValue

trait MockHttp extends Mockito {

  implicit val defaultTimeOut =  Duration(1, TimeUnit.SECONDS)

  type ResponseMocker = (WsResponse) => Unit

  def headersFor(headers:Option[Map[String,String]]): FluentCaseInsensitiveStringsMap = {
    headers.fold (new FluentCaseInsensitiveStringsMap) { data =>
      import scala.collection.JavaConversions._
      new FluentCaseInsensitiveStringsMap(data.map{ case (key, value) => (key, ju.Arrays.asList(value))})
    }
  }

  implicit def pactResponse(p:PactResponse):ResponseMocker = (r) => {
    val ahcResponse = mock[AhcResponse]
    ahcResponse.getHeaders returns headersFor(p.headers)
    r.getAHCResponse returns ahcResponse

    r.status returns p.status
    p.body.map(r.body returns _)
  }

  def stubWs(http:HttpCalls, uri:String, json:JsValue)(responseCode:Int) = {
    val wsRequest = mock[WSRequestHolder]
    println("mocking URI:" + uri)
    http.url(uri) returns wsRequest
    val fakeResponse = mock[WsResponse]
    fakeResponse.status returns responseCode
    http.post(wsRequest, json) returns Future.successful(fakeResponse)
    wsRequest
  }

  def stubWs(http:HttpCalls, uri:String, request:Request)(implicit extractor:ResponseMocker) = {
    val wsRequest = mock[WSRequestHolder]
    println("mocking URI:" + uri)
    http.url(uri) returns wsRequest
    request.headers.map{headers:Map[String,String] => wsRequest.withHeaders(headers.toList:_*) returns wsRequest}
    //TODO: work out how to support query strings
//    wsRequest.withQueryString(any[(String,String)]) returns wsRequest

    val fakeResponse = mock[WsResponse]
    extractor(fakeResponse)
    request.method match {
      case "get" => http.get(wsRequest) returns Future.successful(fakeResponse)
      case "post" => http.post(wsRequest, request.body.get) returns Future.successful(fakeResponse)
      case "put" => http.put(wsRequest, request.body.get) returns Future.successful(fakeResponse)
      case _ => Unit
    }
    wsRequest
  }
}