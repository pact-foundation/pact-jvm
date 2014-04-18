package au.com.dius.pact.consumer

import au.com.dius.pact.model._
import _root_.unfiltered.request._
import _root_.unfiltered.response._

import _root_.unfiltered.netty._
import au.com.dius.pact.model.unfiltered.Conversions

object MockServiceProvider {

  def apply(config: PactServerConfig, pact: Pact, state: String): StoppedMockServiceProvider = {
    StoppedMockServiceProvider(config, pact, state)
  }
  
  class InteractionStore {
    private var interactions: Seq[Interaction] = Seq()
    
    def addInteraction(interaction: Interaction) {
      interactions = interactions :+ interaction
    }
    
    def currentInteractions: Seq[Interaction] = {
      interactions
    }
  }

  case class StoppedMockServiceProvider(config: PactServerConfig, pact: Pact, state: String) {
    case class Routes(interactions: InteractionStore) extends cycle.Plan
      with cycle.SynchronousExecution
      with ServerErrorResponse {

        import org.jboss.netty.handler.codec.http.{ HttpResponse=>NHttpResponse }

        def handle(request:HttpRequest[ReceivedMessage]): ResponseFunction[NHttpResponse] = {
          import RequestMatching._
          val pactRequest: Request = Conversions.unfilteredRequestToPactRequest(request)
          val response: Response = pact.findResponse(pactRequest).getOrElse(Response.invalidRequest(pactRequest, pact))
          interactions.addInteraction(Interaction("MockServiceProvider received", state, pactRequest, response))
          Conversions.pactToUnfilteredResponse(response)
        }
        def intent = PartialFunction[HttpRequest[ReceivedMessage], ResponseFunction[NHttpResponse]](handle)
    }

    def start: StartedMockServiceProvider = {
      val interactionStore = new InteractionStore()
      val server = _root_.unfiltered.netty.Http(config.port, config.interface).handler(Routes(interactionStore))
      println(s"starting server on: ${config.url}")
      server.start()
      StartedMockServiceProvider(config, pact, state, server, interactionStore)
    }
  }

  case class StartedMockServiceProvider(config: PactServerConfig, 
                                        pact: Pact,
                                        state: String,
                                        server: Http,
                                        interactionStore: InteractionStore) {
    def stop: StoppedMockServiceProvider = {
      server.stop()
      StoppedMockServiceProvider(config, pact, state)
    }

    def interactions: Iterable[Interaction] = {
      interactionStore.currentInteractions
    }
  }
}
