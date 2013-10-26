package util

import play.api.libs.json._
import play.api.libs.ws.{Response => WsResponse}
import scala.concurrent.Future
import com.dius.pact.runner.HttpCalls
import org.specs2.mock.Mockito
import scala.xml.Elem
import play.api.libs.concurrent.Execution.Implicits._
import play.api.http.{ContentTypeOf, Writeable}
import java.io.File
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import com.ning.http.client.{Response => AhcResponse, FluentCaseInsensitiveStringsMap}
import com.dius.pact.model.{Response => PactResponse}
import scala.Predef._
import play.api.libs.ws.WS.WSRequestHolder
import java.{util => ju}

trait MockHttp extends Mockito {

  implicit val defaultTimeOut =  Duration(1, TimeUnit.SECONDS)

  type ResponseMocker = (WsResponse) => Unit
  type Action = (WSRequestHolder, WsResponse) => Unit

  val get:Action = (rh, r) => {
    rh.get() returns Future { r }
  }

  val delete:Action = (rh, r) => {
    rh.delete() returns Future { r }
  }

  val head:Action = (rh, r) => {
    rh.head() returns Future { r }
  }

  //TODO: either loosen the mockito matchers, or use a type parameter
  val post:Action = (rh, r) => {
    rh.post(any[Elem])(any[Writeable[Elem]], any[ContentTypeOf[Elem]]) returns Future { r }
  }

  val put:Action = (rh, r) => {
    rh.put(any[Elem])(any[Writeable[Elem]], any[ContentTypeOf[Elem]]) returns Future { r }
  }

  val postFile:Action = (rh, r) => {
    rh.post(any[File]) returns Future { r }
  }

  def headersFor(headers:Option[Map[String,String]]): FluentCaseInsensitiveStringsMap = {
    headers.fold (new FluentCaseInsensitiveStringsMap) { data =>
      import scala.collection.JavaConversions._
      new FluentCaseInsensitiveStringsMap(data.map{ case (key, value) => (key, ju.Arrays.asList(value))})
    }
  }

  implicit def pactResponse(p:PactResponse):ResponseMocker = (r) => {
    println(s"mocking response with headers: ${p.headers}")
    val ahcResponse = mock[AhcResponse]
    ahcResponse.getHeaders returns headersFor(p.headers)
    r.getAHCResponse returns ahcResponse

    r.status returns p.status
    p.body.map(r.body returns _)
  }

  implicit def basicResponseCode(code:Int):ResponseMocker = (r) => {
    r.status returns code
  }

  implicit def jsonSuccess(j:JsValue):ResponseMocker = (r) => {
    r.status returns 200
    r.json returns j
  }

  implicit def jsonFail(errorCode:String):ResponseMocker = (r) => {
    jsonFail(404, errorCode)
  }

  implicit def jsonFail(result:(Int, String)):ResponseMocker = (r) => {
    r.status returns result._1
    r.json returns Json.obj(
      "errorCode"         -> result._2,
      "errorDescription"  -> "Test JSON Error")
  }

  def stubWs(http:HttpCalls, uri:String, action:Action, requestHeaders:Option[Seq[(String,String)]] = None)(implicit extractor:ResponseMocker) = {
    val fakeRequestHolder = mock[WSRequestHolder]
    val fakeResponse = mock[WsResponse]
    extractor(fakeResponse)
    System.err.println("uri is:" + uri)
    http.url(uri) returns fakeRequestHolder
    requestHeaders.map(fakeRequestHolder.withHeaders(_:_*) returns fakeRequestHolder)
    fakeRequestHolder.withQueryString(any[(String,String)]) returns fakeRequestHolder
    action(fakeRequestHolder, fakeResponse)
    fakeRequestHolder
  }

  def anyHeaders(rh:WSRequestHolder) = {
    rh.withHeaders(any[(String,String)]) returns rh
    rh
  }
}