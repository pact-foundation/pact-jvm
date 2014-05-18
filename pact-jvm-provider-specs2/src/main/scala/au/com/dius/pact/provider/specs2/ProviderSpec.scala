package au.com.dius.pact.provider.specs2

import org.specs2.Specification
import org.specs2.execute.Result
import org.specs2.specification.{Fragment, Example, Fragments}
import au.com.dius.pact.model.{FullResponseMatch, ResponseMatching, Pact}
import scala.concurrent.{ExecutionContext, Await}
import au.com.dius.pact.model.dispatch.HttpClient
import scala.concurrent.duration.Duration
import org.json4s.JsonInput
import java.util.concurrent.Executors

trait ProviderSpec extends Specification {

  def timeout = Duration.apply(10000, "s")

  override def is: Fragments = {
    val pact = Pact.from(honoursPact)

    val fragments:Seq[Fragment] = pact.interactions.map { interaction =>
      val description = s"${interaction.providerState} ${interaction.description}"
      val test: String => Result = { url =>
        implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
        val actualResponseFuture = HttpClient.run(interaction.request.copy(path = s"$url${interaction.request.path}"))
        val actualResponse = Await.result(actualResponseFuture, timeout)
        ResponseMatching.matchRules(interaction.response, actualResponse) must beEqualTo(FullResponseMatch)
      }
      Example(description,{inState(interaction.providerState, test)})
    }

    Fragments.create(fragments :_*)
  }

  def honoursPact: JsonInput

  def inState(state: String, test: String => Result): Result

}