package au.com.dius.pact.consumer

import au.com.dius.pact.model._
import com.typesafe.scalalogging.StrictLogging

import scala.util.Try

trait MockProvider {
  def config: MockProviderConfig
  def session: PactSession
  def runAndClose[T](pact: Pact)(code: => T): Try[(T, PactSessionResults)]
}

object DefaultMockProvider {
  
  def withDefaultConfig(pactConfig: PactConfig = PactConfig(PactSpecVersion.V2)) = apply(MockProviderConfig.createDefault(pactConfig))
  
  // Constructor providing a default implementation of StatefulMockProvider.
  // Users should not explicitly be forced to choose a variety.
  def apply(config: MockProviderConfig): StatefulMockProvider = 
    new UnfilteredMockProvider(config)
}

// TODO: eliminate horrid state mutation and synchronisation.  Reactive stuff to the rescue?
abstract class StatefulMockProvider extends MockProvider with StrictLogging {
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
  
  override def runAndClose[T](pact: Pact)(code: => T): Try[(T, PactSessionResults)] = {
    def waitForRequestsToFinish() = Thread.sleep(100)
    Try {
      try {
        start(pact)
        val codeResult = code
        waitForRequestsToFinish()
        (codeResult, session.remainingResults)
      } finally {
        stop()
      }
    }
  }
  
  final def handleRequest(req: Request): Response = synchronized {
    logger.debug("Received request: " + req)
    val (response, newSession) = session.receiveRequest(req)
    logger.debug("Generating response: " + response)
    sessionVar = newSession
    response
  }
}

