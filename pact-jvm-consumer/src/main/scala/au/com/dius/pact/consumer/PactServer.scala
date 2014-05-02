package au.com.dius.pact.consumer

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.{RequestMatch, FullRequestMatch, PartialRequestMatch, RequestMismatch}
import au.com.dius.pact.model.RequestMatching
import au.com.dius.pact.model.Response
import scala.util.Try

trait PactServer {
  def config: PactServerConfig
  def runAndClose(pact: Pact)(code: => Unit): Try[PactSessionResults]
}

  
object DefaultPactServer {
  
  def withDefaultConfig() = apply(PactServerConfig.createDefault())
  
  // Constructor providing a default implementation of StatePactServer.
  // Users should not explicitly be forced to choose a variety.
  def apply(config: PactServerConfig): PactServer = 
    new UnfilteredPactServer(config)
}

// TODO: eliminate horrid state mutation and synchronisation.  Reactive stuff to the rescue?
abstract class StatefulPactServer extends PactServer {
  private var sessionVar = PactSession.empty
  def session: PactSession  = sessionVar
  
  protected def stop(): Unit
  protected def start(): Unit
  
  override def runAndClose(pact: Pact)(code: => Unit): Try[PactSessionResults] = {
    def waitForRequestsToFinish() = Thread.sleep(50)
    sessionVar = PactSession.forPact(pact)
    Try {
      start()
      code
      waitForRequestsToFinish()
      stop()
      session.withTheRestMissing.results
    } 
  }
  
  final def handleRequest(req: Request): Response = synchronized {
    val (response, newSession) = session.receiveRequest(req)
    sessionVar = newSession
    response
  }
}

