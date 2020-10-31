package au.com.dius.pact.server

import _root_.unfiltered.netty.{cycle => unettyc}
import _root_.unfiltered.{netty => unetty, request => ureq, response => uresp}
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.{Request, Response}
import io.netty.channel.ChannelHandler.Sharable
import io.netty.handler.codec.{http => netty}

class UnfilteredMockProvider(val config: MockProviderConfig) extends StatefulMockProvider {
  type UnfilteredRequest = ureq.HttpRequest[unetty.ReceivedMessage]
  type UnfilteredResponse = uresp.ResponseFunction[netty.HttpResponse]

  private val server = unetty.Server.http(config.getPort, config.getHostname).chunked(1048576).handler(Routes)

  @Sharable
  object Routes extends unettyc.Plan
    with unettyc.SynchronousExecution
    with unetty.ServerErrorResponse {

    override def intent: unettyc.Plan.Intent = {
      case req => convertResponse(handleRequest(convertRequest(req)))
    }

    def convertRequest(nr: UnfilteredRequest): Request = Conversions.unfilteredRequestToPactRequest(nr)

    def convertResponse(response: Response): UnfilteredResponse = Conversions.pactToUnfilteredResponse(response)
  }

  def start(): Unit = server.start()

  def stop(): Unit = server.stop()
}
