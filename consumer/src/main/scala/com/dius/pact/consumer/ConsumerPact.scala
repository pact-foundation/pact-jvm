package com.dius.pact.consumer

import com.dius.pact.author._
import akka.actor.ActorSystem
import com.dius.pact.author.PactServerConfig
import com.dius.pact.model.Pact
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class ConsumerPact(pact: Pact) {
  def execute(test: => Unit): Try[Unit] = Try(test)

  def runConsumer(config: PactServerConfig,state: String)
                 (test: => Boolean)
                 (implicit system: ActorSystem = ActorSystem("default-pact-consumer-actor-system")):
      Future[PactVerification.VerificationResult] = {
    implicit val executionContext = system.dispatcher

    for {
      started <- MockServiceProvider(config, pact).start
      inState <- started.enterState(state)
      result = execute(test)
      actualInteractions <- inState.interactions
      verified = PactVerification(pact.interactions, actualInteractions)(result)
      fileWrite = PactGeneration(pact, verified)
      stopped <- inState.stop
    } yield { verified }
  }
}

object ConsumerPact {
  implicit def pimp(pact: Pact) = ConsumerPact(pact)
}
