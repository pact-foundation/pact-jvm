package com.dius.pact.runner

import com.dius.pact.model.Request
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.ws.{Response => WsResponse}
import com.dius.pact.model.Response
import scala.concurrent.{ExecutionContext, Future}

class Service(baseUrl:String, http:HttpCalls)(implicit executionContext: ExecutionContext) {

  //TODO: this method is a symptom of poor data modeling, Post and Get are different types of request
  def action(verb:String, holder:WSRequestHolder, body:Option[String]):Future[WsResponse] = {
    println("holder="+holder)
    verb match {
      case "get" => holder.get()
      case "post" => holder.post(body.get)
      case "put" => holder.post(body.get)
      case _ => {throw new RuntimeException("unknown httpVerb")}
    }
  }

  type Action = (WSRequestHolder, Option[String]) => Future[WsResponse]
  
  def headers(holder:WSRequestHolder, headers:Map[String,String]):WSRequestHolder = {
    holder.withHeaders(headers.toList :_*)
  }

  def invoke(request:Request):Future[Response] = {
    val url = s"${baseUrl}${request.path}"
    println("url:"+url)
    val holder = http.url(url)
    val readyToGo = request.headers.fold (holder) (headers(holder, _))
    action(
      request.method.toLowerCase,
      readyToGo,
      request.body
    ).map { wsResponse =>
      import HttpCalls._
      val headers = wsResponse.allHeaders.map{ case (key, value) => (key, value.head)}
      Response(wsResponse.status, headers, wsResponse.body)
    }
  }


}
