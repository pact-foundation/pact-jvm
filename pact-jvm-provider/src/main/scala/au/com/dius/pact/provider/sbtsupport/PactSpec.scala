package au.com.dius.pact.provider.sbtsupport

import java.util.concurrent.Executors

import au.com.dius.pact.model._
import au.com.dius.pact.model.dispatch.HttpClient
import au.com.dius.pact.provider.{EnterStateRequest, ServiceInvokeRequest}
import org.scalatest.exceptions.TestFailedException
import org.scalatest.{Assertions, FreeSpec}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class PactSpec(config: PactConfiguration, pact: Pact)(implicit timeout: Duration = 10.seconds) extends FreeSpec with Assertions {
  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  pact.interactions.toList.foreach { interaction =>
    s"""pact for consumer ${pact.consumer.name} 
       |provider ${pact.provider.name} 
       |interaction "${interaction.description}"
       |in state: "${interaction.providerState.getOrElse("")}" """.stripMargin in {

        val stateChangeFuture = (Option.apply(config.getStateChangeUrl), interaction.providerState) match {
          case (Some(stateChangeUrl), Some(providerState)) => HttpClient.run(EnterStateRequest(stateChangeUrl.url, providerState))
          case (_, _) => Future.successful(Response(200, None, None, None))
        }
        
        val pactResponseFuture: Future[Response] = for {
          _ <- stateChangeFuture
          response <- HttpClient.run(ServiceInvokeRequest(config.getProviderRoot.url, interaction.request))
        } yield response

        val actualResponse = Await.result(pactResponseFuture, timeout)

      val responseMismatches = ResponseMatching.responseMismatches(interaction.response, actualResponse)
      if (!responseMismatches.isEmpty) {
          throw new TestFailedException(s"There were response mismatches: \n${responseMismatches.mkString("\n")}", 10)
        }
      }
  }
}
