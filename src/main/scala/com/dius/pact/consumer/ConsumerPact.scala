package com.dius.pact.consumer

import com.dius.pact.author._
import akka.actor.ActorSystem
import com.dius.pact.author.PactServerConfig
import com.dius.pact.model.Pact
import scala.concurrent.Future

case class ConsumerPact(pact: Pact) {
  def runConsumer[T](config: PactServerConfig, state: String)(test: => T)(implicit system: ActorSystem = ActorSystem()): Future[PactVerification.PactVerification] = {
    implicit val executionContext = system.dispatcher

    for {
      started <- FakeProviderServer(config, pact).start
      inState <- started.enterState(state)
      result = test
      verified <- PactVerification(pact, inState)
      stopped <- inState.stop
    } yield { verified }
  }
}

object ConsumerPact {
  implicit def pimp(pact: Pact) = ConsumerPact(pact)
}
