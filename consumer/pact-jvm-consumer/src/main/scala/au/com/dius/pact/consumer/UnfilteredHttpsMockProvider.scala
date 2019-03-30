package au.com.dius.pact.consumer

import au.com.dius.pact.model.unfiltered.Conversions
import au.com.dius.pact.model._
import io.netty.channel.ChannelHandler.Sharable
import io.netty.handler.codec.{http => netty}
import _root_.unfiltered.netty.{SslContextProvider, cycle => unettyc}
import _root_.unfiltered.{netty => unetty, request => ureq, response => uresp}
import au.com.dius.pact.core.model.{Request, RequestResponseInteraction, Response}

class UnfilteredHttpsMockProvider(val config: MockHttpsProviderConfig) extends StatefulMockProvider[RequestResponseInteraction] {
  type UnfilteredRequest = ureq.HttpRequest[unetty.ReceivedMessage]
  type UnfilteredResponse = uresp.ResponseFunction[netty.HttpResponse]

  def sslContext: SslContextProvider = SslContextProvider.selfSigned(config.getHttpsCertificate)

  private val server = unetty.Server.https(config.getPort, config.getHostname, sslContext).chunked(1048576).handler(Routes)
  
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
