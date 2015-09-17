package au.com.dius.pact.provider.specs2

import java.util.concurrent.Executors

import au.com.dius.pact.model.dispatch.HttpClient
import au.com.dius.pact.model.{FullResponseMatch, Pact, ResponseMatching}
import org.json4s.JsonInput
import org.specs2.Specification
import org.specs2.execute.Result
import org.specs2.specification.core.Fragments

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

trait ProviderSpec extends Specification {

  def timeout = Duration.apply(10000, "s")

  override def is = {
    val pact = Pact.from(honoursPact)
    val fs = pact.interactions.map { interaction =>
      val description = s"${interaction.providerState} ${interaction.description}"
      val test: String => Result = { url =>
        implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
        val actualResponseFuture = HttpClient.run(interaction.request.copy(path = s"$url${interaction.request.path}"))
        val actualResponse = Await.result(actualResponseFuture, timeout)
        ResponseMatching.matchRules(interaction.response, actualResponse) must beEqualTo(FullResponseMatch)
      }
      fragmentFactory.example(description, {inState(interaction.providerState.get, test)})
    }
    Fragments(fs :_*)
  }

  def honoursPact: JsonInput

  def inState(state: String, test: String => Result): Result

}
