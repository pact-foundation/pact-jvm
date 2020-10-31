package au.com.dius.pact.server

import _root_.unfiltered.netty.{SslEngineProvider, cycle => unettyc}
import _root_.unfiltered.{netty => unetty, request => ureq, response => uresp}
import au.com.dius.pact.consumer.model.MockHttpsKeystoreProviderConfig
import au.com.dius.pact.core.model.{Request, Response}
import io.netty.channel.ChannelHandler.Sharable
import io.netty.handler.codec.{http => netty}

class UnfilteredHttpsKeystoreMockProvider(val config: MockHttpsKeystoreProviderConfig) extends StatefulMockProvider {
  type UnfilteredRequest = ureq.HttpRequest[unetty.ReceivedMessage]
  type UnfilteredResponse = uresp.ResponseFunction[netty.HttpResponse]

  //def sslEngine: SslEngineProvider = SslEngineProvider.pathSysProperties()
  def sslEngine: SslEngineProvider = SslEngineProvider.path(config.getKeystore, config.getPassword)
  private val server = unetty.Server.httpsEngine(config.getPort, config.getHostname, sslEngine).chunked(1048576).handler(Routes)

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
