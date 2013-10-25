package util

import play.api.libs.json._
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.ws.Response
import scala.concurrent.Future
import com.dius.pact.runner.HttpCalls
import org.specs2.mock.Mockito
import scala.xml.Elem
import play.api.libs.concurrent.Execution.Implicits._
import play.api.http.{ContentTypeOf, Writeable}
import java.io.File
import org.specs2.matcher.Hamcrest.eq

trait MockHttp extends Mockito {

  type ResponseMocker = (Response) => Unit
  type Action = (WSRequestHolder, Response) => Unit

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

  def stubWs(http:HttpCalls, uri:String, action:Action)(implicit extractor:ResponseMocker) = {
    val fakeRequestHolder = mock[WSRequestHolder]
    val fakeResponse = mock[Response]
    extractor(fakeResponse)
    System.err.println("uri is:" + uri)
    http.url(uri) returns fakeRequestHolder
    fakeRequestHolder.withQueryString(any[(String,String)]) returns fakeRequestHolder
    action(fakeRequestHolder, fakeResponse)
    fakeRequestHolder
  }

  def anyHeaders(rh:WSRequestHolder) = {
    rh.withHeaders(any[(String,String)]) returns rh
    rh
  }
}