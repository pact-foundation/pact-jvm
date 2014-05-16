package au.com.dius.pact.server

import org.jboss.netty.handler.codec.http.{HttpResponse => NHttpResponse}
import au.com.dius.pact.model.unfiltered.Conversions
import unfiltered.netty.ReceivedMessage
import unfiltered.netty.ServerErrorResponse
import unfiltered.netty.cycle
import unfiltered.request.HttpRequest
import unfiltered.response.ResponseFunction
import scala.collection.immutable.Map


class ServerStateStore {
  var state: ServerState = Map()
}


case class RequestHandler(store: ServerStateStore) extends cycle.Plan
  with cycle.SynchronousExecution
  with ServerErrorResponse {
    import org.jboss.netty.handler.codec.http.{ HttpResponse=>NHttpResponse }

    def handle(request: HttpRequest[ReceivedMessage]): ResponseFunction[NHttpResponse] = {
      val pactRequest = Conversions.unfilteredRequestToPactRequest(request)
      val result = RequestRouter.dispatch(pactRequest, store.state)
      store.state = result.newState
      Conversions.pactToUnfilteredResponse(result.response)
    }
    def intent = PartialFunction[HttpRequest[ReceivedMessage], ResponseFunction[NHttpResponse]](handle)
}
