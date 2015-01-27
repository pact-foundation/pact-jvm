package au.com.dius.pact.provider

import org.scalatest.{Assertions, FreeSpec}
import org.scalatest.exceptions.TestFailedException
import au.com.dius.pact.model._
import au.com.dius.pact.model.dispatch.HttpClient
import scala.concurrent.{ExecutionContext, Await, Future}
import scala.concurrent.duration._
import java.util.concurrent.Executors

class PactSpec(config: PactConfiguration, pact: Pact)(implicit timeout: Duration = 10.seconds) extends FreeSpec with Assertions {
  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  pact.interactions.toList.map { interaction =>
    s"""pact for consumer ${pact.consumer.name} 
       |provider ${pact.provider.name} 
       |interaction "${interaction.description}"
       |in state: "${interaction.providerState.getOrElse("")}" """.stripMargin in {

        val stateChangeFuture = (config.stateChangeUrl, interaction.providerState) match {
          case (Some(stateChangeUrl), Some(providerState)) => HttpClient.run(EnterStateRequest(stateChangeUrl.url, providerState))
          case (_, _) => Future()
        }
        
        val pactResponseFuture: Future[Response] = for {
          _ <- stateChangeFuture
          response <- HttpClient.run(ServiceInvokeRequest(config.providerRoot.url, interaction.request))
        } yield response

        val actualResponse = Await.result(pactResponseFuture, timeout)

      val responseMismatches = ResponseMatching.responseMismatches(interaction.response, actualResponse)
      if (!responseMismatches.isEmpty) {
          throw new TestFailedException(s"There were response mismatches: \n${responseMismatches.mkString("\n")}", 10)
        }
      }
  }
}
