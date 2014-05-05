package au.com.dius.pact.consumer

import au.com.dius.pact.model.Pact
import scala.concurrent.{Await, Future}
import scala.util.Try
import scala.concurrent.duration._

case class ConsumerPact(pact: Pact) {
  def execute(test: => Unit): Try[Unit] = Try(test)

  def runConsumer(config: MockProviderConfig, state: String)
                 (test: => Unit):
      Future[PactVerification.VerificationResult] = {
      val started = MockServiceProvider(config, pact, state).start
      val result = execute(test)
      val actualInteractions = started.interactions
      val verified = PactVerification(pact.interactions, actualInteractions, result)
      val fileWriteVerification = PactGeneration(pact, verified)
      val stopped = started.stop
      Future.successful(fileWriteVerification)
  }

  @deprecated("exists to support au.com.dius.pact.consumer.AbstractConsumerPactTest, which is also deprecated")
  def runConsumer(config: MockProviderConfig, state: String, test: Runnable): PactVerification.VerificationResult = {
      Await.result(runConsumer(config, state) { test.run() }, 20 seconds)
  }

}

object ConsumerPact {
  implicit def pimp(pact: Pact) = ConsumerPact(pact)
}
