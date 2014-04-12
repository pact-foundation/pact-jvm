package au.com.dius.pact.provider

import org.scalatest.{Assertions, FreeSpec}
import au.com.dius.pact.model._
import au.com.dius.pact.model.Matching._
import au.com.dius.pact.model.finagle.Conversions._
import scala.util.control.NonFatal
import org.jboss.netty.handler.codec.http._
import java.nio.charset.Charset
import com.twitter.finagle.Http
import com.twitter.util.Await
import com.twitter.util.Future


class PactSpec(config: PactConfiguration, pact: Pact) extends FreeSpec with Assertions {

  def convert(request: Request, response: HttpResponse): Response = {
    try {
      response
    } catch {
      case NonFatal(e) => throw new RuntimeException(
        s"""Unable to convert response:
           |   status: ${response.getStatus.getCode}
           |   headers: ${response.headers}
           |   body: ${response.getContent.toString(Charset.forName("UTF-8"))}
           |for request:
           |   $request""".stripMargin)
    }
  }

  pact.interactions.toList.map { interaction =>
    s"""pact for consumer ${pact.consumer.name} 
       |provider ${pact.provider.name} 
       |interaction "${interaction.description}"
       |in state: "${interaction.providerState}" """.stripMargin in {

        val http = Http.newService(s"${config.providerRoot.host}:${config.providerRoot.port}")

        
        val changeFuture = config.stateChangeUrl match {
          case Some(stateChangeUrl) => http(EnterStateRequest(stateChangeUrl.url, interaction.providerState))
          case None => Future()
        }
        
        val pactResponseFuture = for {
          _ <- changeFuture
          httpResponse <- http(ServiceInvokeRequest(config.providerRoot.url, interaction.request))
        } yield convert(interaction.request, httpResponse)

        val actualResponse = Await.result(pactResponseFuture)

        assert(ResponseMatching.matchRules(interaction.response, actualResponse) === MatchFound)
      }
  }
}
