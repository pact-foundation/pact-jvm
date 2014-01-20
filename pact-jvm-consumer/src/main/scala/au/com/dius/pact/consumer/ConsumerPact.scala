package au.com.dius.pact.consumer

import akka.actor.ActorSystem
import au.com.dius.pact.model.Pact
import scala.concurrent.{Await, Future}
import scala.util.Try
import scala.concurrent.duration._

case class ConsumerPact(pact: Pact) {
  def execute(test: => Unit): Try[Unit] = Try(test)

  def runConsumer(config: PactServerConfig, state: String)(test: => Unit): Future[PactVerification.VerificationResult] = {
    val system = ActorSystem("default-pact-consumer-actor-system")
    implicit val executionContext = system.dispatcher
    val f = runConsumer(config, state, system)(test)
    f.onComplete {_ => system.shutdown() }
    f
  }

  def runConsumer(config: PactServerConfig, state: String, system: ActorSystem)
                 (test: => Unit):
      Future[PactVerification.VerificationResult] = {
    implicit val actorSystem = system
    implicit val executionContext = system.dispatcher

    for {
      started <- MockServiceProvider(config, pact).start
      inState <- started.enterState(state)
      result = execute(test)
      actualInteractions <- inState.interactions
      verified = PactVerification(pact.interactions, actualInteractions)(result)
      fileWriteVerification = PactGeneration(pact, verified)
      stopped <- inState.stop
    } yield { fileWriteVerification }
  }

  def runConsumer(config: PactServerConfig, state: String, test: Runnable): PactVerification.VerificationResult = {
    val system = ActorSystem("default-pact-consumer-actor-system")
    try {
      //TODO: externalise timeouts
      Await.result(runConsumer(config, state, system) { test.run() }, 20 seconds)
    } finally {
      system.shutdown()
    }
  }
}

object ConsumerPact {
  implicit def pimp(pact: Pact) = ConsumerPact(pact)
}
