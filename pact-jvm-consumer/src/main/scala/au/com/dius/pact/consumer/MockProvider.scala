package au.com.dius.pact.consumer

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.{RequestMatch, FullRequestMatch, PartialRequestMatch, RequestMismatch}
import au.com.dius.pact.model.RequestMatching
import au.com.dius.pact.model.Response
import scala.util.Try
import scala.util.Success

trait MockProvider {
  def config: MockProviderConfig
  def runAndClose(pact: Pact)(code: => Unit): Try[PactSessionResults]
}

object DefaultMockProvider {
  
  def withDefaultConfig() = apply(MockProviderConfig.createDefault())
  
  // Constructor providing a default implementation of StatefulMockProvider.
  // Users should not explicitly be forced to choose a variety.
  def apply(config: MockProviderConfig): StatefulMockProvider = 
    new UnfilteredMockProvider(config)
}

// TODO: eliminate horrid state mutation and synchronisation.  Reactive stuff to the rescue?
abstract class StatefulMockProvider extends MockProvider {
  private var sessionVar = PactSession.empty
  private var pactVar: Option[Pact] = None
  
  def session: PactSession  = sessionVar
  def pact: Option[Pact] = pactVar
  
  def stop(): Unit
  def start(): Unit
  
  final def start(pact: Pact): Unit = synchronized {
    pactVar = Some(pact)
    sessionVar = PactSession.forPact(pact)
    start()
  }
  
  override def runAndClose(pact: Pact)(code: => Unit): Try[PactSessionResults] = {
    def waitForRequestsToFinish() = Thread.sleep(50)
    Try {
      start(pact)
      code
      waitForRequestsToFinish()
      stop()
      session.remainingResults
    } 
  }
  
  final def handleRequest(req: Request): Response = synchronized {
    val (response, newSession) = session.receiveRequest(req)
    sessionVar = newSession
    response
  }
}

