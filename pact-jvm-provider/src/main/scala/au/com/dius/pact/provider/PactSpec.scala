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


class PactSpec(config: PactConfiguration, pact: Pact) extends FreeSpec with Assertions {

  def convert(request: Request, response: HttpResponse): Response = {
    try {
      response
    } catch {
      case NonFatal(e) => throw new RuntimeException(
s"""Unable to convert response:
  status: ${response.getStatus.getCode}
  headers: ${response.headers}
  body: ${response.getContent.toString(Charset.forName("UTF-8"))}
for request:
  $request""")
    }
  }

  pact.interactions.toList.map { interaction =>
    s"""pact for consumer ${pact.consumer.name} """ +
      s"""provider ${pact.provider.name} """ +
      s"""interaction "${interaction.description}" """ +
      s"""in state: "${interaction.providerState}" """ in {

        val http = Http.newService(s"${config.providerRoot.host}:${config.providerRoot.port}")

        val stateResponse = http(EnterStateRequest(config.stateChangeUrl.url, interaction.providerState))

        val httpResponse = stateResponse.flatMap { _ =>
          http(ServiceInvokeRequest(config.providerRoot.url, interaction.request))
        }

        val pactResponse = httpResponse.map { it =>
          convert(interaction.request, it)
        }

        val actualResponse = Await.result(pactResponse)

        assert(ResponseMatching.matchRules(interaction.response, actualResponse) === MatchFound)
      }
  }
}
