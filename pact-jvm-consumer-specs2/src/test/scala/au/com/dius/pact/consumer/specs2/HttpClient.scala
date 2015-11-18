package au.com.dius.pact.consumer.specs2

import au.com.dius.pact.model.unfiltered.Conversions
import au.com.dius.pact.model.{CollectionUtils, Request, Response}
import com.ning.http.client.FluentStringsMap
import dispatch.url

import scala.collection.JavaConversions
import scala.concurrent.{ExecutionContext, Future}

object HttpClient {
  def run(request:Request)(implicit executionContext: ExecutionContext):Future[Response] = {
    val queryMap = CollectionUtils.javaLMapToScalaLMap(request.getQuery)
    val query = queryMap.foldLeft(new FluentStringsMap()) {
      (fsm, q) => q._2.foldLeft(fsm) { (m, a) => m.add(q._1, a ) }
    }
    val headers = JavaConversions.mapAsScalaMap(request.getHeaders)
    val r = url(request.getPath).underlying(
      _.setMethod(request.getMethod).setQueryParameters(query)
    ) <:< headers
    val httpRequest = r.setBody(request.getBody)
    dispatch.Http(httpRequest).map(Conversions.dispatchResponseToPactResponse)
  }
}
