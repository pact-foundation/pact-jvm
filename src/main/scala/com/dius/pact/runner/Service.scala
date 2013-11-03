package com.dius.pact.runner

import com.dius.pact.model.Request
import com.dius.pact.model.Response
import scala.concurrent.{ExecutionContext, Future}
import com.dius.pact.runner.http.Client

class Service(baseUrl:String, http:Client)(implicit executionContext: ExecutionContext) {

  def invoke(request:Request):Future[Response] = {
    http.invoke(s"${baseUrl}", request)
  }
}
