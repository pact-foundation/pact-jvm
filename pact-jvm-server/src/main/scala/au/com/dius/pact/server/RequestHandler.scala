package au.com.dius.pact.server

import io.netty.channel.ChannelHandler.Sharable
import unfiltered.netty.ReceivedMessage
import unfiltered.netty.ServerErrorResponse
import unfiltered.netty.cycle
import unfiltered.request.HttpRequest
import unfiltered.response.ResponseFunction
import scala.collection.immutable.Map

class ServerStateStore {
  var state: ServerState = new ServerState()
}

@Sharable
case class RequestHandler(store: ServerStateStore, config: Config) extends cycle.Plan
  with cycle.SynchronousExecution
  with ServerErrorResponse {
    import io.netty.handler.codec.http.{ HttpResponse=>NHttpResponse }

    def handle(request: HttpRequest[ReceivedMessage]): ResponseFunction[NHttpResponse] = {
      val pactRequest = Conversions.unfilteredRequestToPactRequest(request)
      val result = RequestRouter.dispatch(pactRequest, store.state, config)
      store.state = result.getNewState
      Conversions.pactToUnfilteredResponse(result.getResponse)
    }
    def intent = PartialFunction[HttpRequest[ReceivedMessage], ResponseFunction[NHttpResponse]](handle)
}
