package au.com.dius.pact.provider.scalasupport

import java.util.concurrent.Executors

import au.com.dius.pact.core.matchers._
import au.com.dius.pact.core.model.{RequestResponsePact, Response}
import org.scalatest.exceptions.TestFailedException
import org.scalatest.Assertions
import org.scalatest.freespec.AnyFreeSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters._

class PactSpec(config: PactConfiguration, pact: RequestResponsePact)(implicit timeout: Duration = 10.seconds) extends AnyFreeSpec with Assertions {
  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  pact.getInteractions.asScala.toList.foreach { interaction =>
    s"""pact for consumer ${pact.getConsumer.getName}
       |provider ${pact.getProvider.getName}
       |interaction "${interaction.getDescription}"
       |in state: "${interaction.getProviderStates.get(0)}" """.stripMargin in {

        val stateChangeFuture = (Option.apply(config.getStateChangeUrl), Option.apply(interaction.getProviderStates.get(0))) match {
          case (Some(stateChangeUrl), Some(providerState)) => HttpClient.run(EnterStateRequest(stateChangeUrl.url, providerState.getName))
          case (_, _) => Future.successful(new Response(200))
        }
        
        val pactResponseFuture: Future[Response] = for {
          _ <- stateChangeFuture
          response <- HttpClient.run(ServiceInvokeRequest(config.getProviderRoot.url, interaction.asSynchronousRequestResponse().getRequest))
        } yield response

        val actualResponse = Await.result(pactResponseFuture, timeout)

      val responseMismatches = ResponseMatching.responseMismatches(interaction.asSynchronousRequestResponse().getResponse, actualResponse)
      if (!responseMismatches.isEmpty) {
          throw new TestFailedException(s"There were response mismatches: \n${responseMismatches.asScala.mkString("\n")}", 10)
        }
      }
  }
}
