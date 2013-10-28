package com.dius.pact.runner.http

import com.dius.pact.model.{Response, Request}
import scala.concurrent.Future
import play.api.libs.json.JsValue

class Client {
  def invoke(baseUrl:String, request:Request):Future[Response ]= ???

  def invoke(baseUrl:String, body:JsValue):Future[Boolean] = ???
}
