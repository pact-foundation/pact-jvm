package au.com.dius.pact.model.dispatch

import au.com.dius.pact.model.{Response, Request}
import scala.collection.JavaConversions
import scala.concurrent.{ExecutionContext, Future}
import dispatch.url
import au.com.dius.pact.model.unfiltered.Conversions
import com.ning.http.client.FluentStringsMap

object HttpClient {
  def run(request:Request)(implicit executionContext: ExecutionContext):Future[Response] = {
    val query = request.query.foldLeft(new FluentStringsMap()) {
      (fsm, q) => q.foldLeft(fsm) { (m, a) => m.add(a._1, JavaConversions.asJavaCollection(a._2) ) }
    }
    val r = url(request.path).underlying(
      _.setMethod(request.method).setQueryParameters(query)
    ) <:< request.headers.getOrElse(Map())
    val httpRequest = request.body.fold(r){ b => r.setBody(b) }
    dispatch.Http(httpRequest).map(Conversions.dispatchResponseToPactResponse)
  }
}
