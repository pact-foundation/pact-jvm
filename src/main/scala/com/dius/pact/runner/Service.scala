package com.dius.pact.runner

import com.dius.pact.model.Request
import com.dius.pact.model.Response
import scala.concurrent.{ExecutionContext, Future}

class Service(baseUrl:String, http:HttpCalls)(implicit executionContext: ExecutionContext) {

  def invoke(request:Request):Future[Response] = {
    val url = s"${baseUrl}${request.path}"
    
    val wsRequest = http.url(url)
    request.headers.fold (wsRequest) {headers:Map[String,String] => wsRequest.withHeaders(headers.toList:_*)}
    val futureResponse = request.method match {
      case "get" => http.get(wsRequest)
      case "post" => http.post(wsRequest, request.body.get)
      case "put" => http.put(wsRequest, request.body.get)
      case _ => {throw new RuntimeException("unknown httpVerb")}
    }

    futureResponse.map { wsResponse =>
      import HttpCalls._
      val headers = wsResponse.allHeaders.map{ case (key, value) => (key, value.head)}
      Response(wsResponse.status, headers, wsResponse.body)
    }
  }
}
