package au.com.dius.pact.provider.sbtsupport

import au.com.dius.pact.model.{Request, Response}
import com.typesafe.scalalogging.StrictLogging
import org.asynchttpclient.RequestBuilder

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConversions._

object HttpClient extends StrictLogging {

  def run(request: Request)(implicit ec: ExecutionContext): Future[Response] = {
    logger.debug("request=" + request)
    val req = new RequestBuilder(request.getMethod)
      .setUrl(request.getPath)
      .setQueryParams(request.getQuery)
    if (request.getHeaders != null) {
      for ((name, header) <- request.getHeaders) {
        req.addHeader(name, header)
      }
    }
    if (request.getBody.isPresent) {
      req.setBody(request.getBody.getValue)
    }

    Future {
      HttpClientSupport.INSTANCE.executeRequest(req).get()
    }
  }
}
