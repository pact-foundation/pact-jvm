package au.com.dius.pact.consumer

import io.netty.handler.codec.{http => netty}

import au.com.dius.pact.model.{MockProviderConfig, Request, Response}
import au.com.dius.pact.model.unfiltered.Conversions
import unfiltered.{netty => unetty}
import unfiltered.netty.{cycle => unettyc}
import unfiltered.{request => ureq}
import unfiltered.{response => uresp}
import io.netty.channel.ChannelHandler.Sharable

class UnfilteredMockProvider(val config: MockProviderConfig) extends StatefulMockProvider {
  type UnfilteredRequest = ureq.HttpRequest[unetty.ReceivedMessage]
  type UnfilteredResponse = uresp.ResponseFunction[netty.HttpResponse]

  private val server = unetty.Server.http(config.port, config.interface).chunked(1048576).handler(Routes)
  
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


