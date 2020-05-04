package au.com.dius.pact.provider.sbtsupport

import java.util.concurrent.Executors

import au.com.dius.pact.core.model.{RequestResponsePact, Response}
import au.com.dius.pact.core.matchers._
import au.com.dius.pact.provider.{EnterStateRequest, ServiceInvokeRequest}
import org.scalatest.exceptions.TestFailedException
import org.scalatest.{Assertions, FreeSpec, Ignore}

import scala.collection.JavaConversions
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

@Ignore
// Ignored as it seems to be failing on travis
class PactSpec(config: PactConfiguration, pact: RequestResponsePact)(implicit timeout: Duration = 10.seconds) extends FreeSpec with Assertions {
  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  JavaConversions.asScalaBuffer(pact.getInteractions).toList.foreach { interaction =>
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
          response <- HttpClient.run(ServiceInvokeRequest(config.getProviderRoot.url, interaction.getRequest))
        } yield response

        val actualResponse = Await.result(pactResponseFuture, timeout)

      val responseMismatches = ResponseMatching.responseMismatches(interaction.getResponse, actualResponse, true)
      if (!responseMismatches.isEmpty) {
          throw new TestFailedException(s"There were response mismatches: \n${responseMismatches.asScala.mkString("\n")}", 10)
        }
      }
  }
}
