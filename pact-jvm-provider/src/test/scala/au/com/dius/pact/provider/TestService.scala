package au.com.dius.pact.provider

import au.com.dius.pact.model.finagle.Conversions._
import AnimalServiceResponses.responses
import com.twitter.finagle.{Service, Http}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}
import com.twitter.util.Future
import au.com.dius.pact.model.{Request, Response}
import org.json4s.JsonAST.{JObject, JField, JString}
import com.twitter.finagle.ListeningServer

object TestService {
  var state: String = ""

  def apply(port:Int): ListeningServer = {
    Http.serve(s":$port", service(port))
  }

  def service(port: Int) = new Service[HttpRequest, HttpResponse] {
    def apply( request: HttpRequest): Future[HttpResponse] = {
      Future{
        if(request.getUri.endsWith("/enterState")) {
          val pactRequest: Request = request
//          println(s"entering state: ${pactRequest.bodyString}")
          pactRequest.body.map {
            case JObject(List(JField("state", JString(s)))) => state = s
          }
          Response(200, None, None)
        } else {
//          println(s"getting: $state ${request.path}")
          val response: Response = responses.get(state).flatMap(_.get(request.path)).getOrElse(Response(400, None, None))
          response: HttpResponse
        }
      }
    }
  }
}
