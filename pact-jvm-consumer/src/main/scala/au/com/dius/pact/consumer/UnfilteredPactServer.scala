package au.com.dius.pact.consumer

import org.jboss.netty.handler.codec.{http => netty}

import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.Response
import au.com.dius.pact.model.unfiltered.Conversions
import unfiltered.{netty => unetty}
import unfiltered.netty.{cycle => unettyc}
import unfiltered.{request => ureq}
import unfiltered.{response => uresp}

class UnfilteredPactServer(val config: PactServerConfig) extends StatefulPactServer {
  type UnfilteredRequest = ureq.HttpRequest[unetty.ReceivedMessage]
  type UnfilteredResponse = uresp.ResponseFunction[netty.HttpResponse]
  
  private val server = unetty.Http(config.port, config.interface).handler(Routes)
  
  object Routes extends unettyc.Plan
      with unettyc.SynchronousExecution
      with unetty.ServerErrorResponse {

      override def intent: unettyc.Plan.Intent = {
        case req => convertResponse(handleRequest(convertRequest(req)))
      }
      
      def convertRequest(nr: UnfilteredRequest): Request = 
        Conversions.unfilteredRequestToPactRequest(nr)
        
      def convertResponse(response: Response): UnfilteredResponse = 
        Conversions.pactToUnfilteredResponse(response)
  }
  
  def stop(): Unit = server.start()
  def start(): Unit = server.stop()
}


