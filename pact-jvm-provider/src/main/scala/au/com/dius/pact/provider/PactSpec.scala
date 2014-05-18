package au.com.dius.pact.provider

import org.scalatest.{Assertions, FreeSpec}
import au.com.dius.pact.model._
import au.com.dius.pact.model.Matching._
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
       |in state: "${interaction.providerState}" """.stripMargin in {

        val stateChangeFuture = config.stateChangeUrl match {
          case Some(stateChangeUrl) => HttpClient.run(EnterStateRequest(stateChangeUrl.url, interaction.providerState))
          case None => Future()
        }
        
        val pactResponseFuture: Future[Response] = for {
          _ <- stateChangeFuture
          response <- HttpClient.run(ServiceInvokeRequest(config.providerRoot.url, interaction.request))
        } yield response

        val actualResponse = Await.result(pactResponseFuture, timeout)

        assert(ResponseMatching.matchRules(interaction.response, actualResponse) === FullResponseMatch)
      }
  }
}
