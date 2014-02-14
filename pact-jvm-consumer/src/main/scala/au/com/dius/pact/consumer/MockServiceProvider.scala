package au.com.dius.pact.consumer

import au.com.dius.pact.model._
import au.com.dius.pact.model.finagle.Conversions._
import com.twitter.finagle.{ListeningServer, Service, Http}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}
import com.twitter.util.Future

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
    def start: StartedMockServiceProvider = {
      val interactionStore = new InteractionStore()
      val server = Http.serve(s":${config.port}", routes(interactionStore))
      StartedMockServiceProvider(config, pact, state, server, interactionStore)
    }

    def routes(interactions: InteractionStore) = new Service[HttpRequest, HttpResponse] {
      def apply(request: HttpRequest): Future[HttpResponse] = {
        import RequestMatching._
        val response: Response = pact.findResponse(request).getOrElse(Response.invalidRequest(request, pact))
        interactions.addInteraction(Interaction("MockServiceProvider received", state, request, response))
        Future(response)
      }
    }
  }

  case class StartedMockServiceProvider(config: PactServerConfig, 
                                        pact: Pact,
                                        state: String,
                                        server: ListeningServer, 
                                        interactionStore: InteractionStore) {
    def stop: StoppedMockServiceProvider = {
      server.close()
      StoppedMockServiceProvider(config, pact, state)
    }

    def interactions: Iterable[Interaction] = {
      interactionStore.currentInteractions
    }
  }
}
