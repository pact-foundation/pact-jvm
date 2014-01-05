package com.dius.pact.runner

import scala.concurrent.{Await, ExecutionContext}
import org.scalatest.{Assertions, Sequential, FreeSpec}
import scala.concurrent.duration.Duration
import com.dius.pact.model._
import com.dius.pact.model.Matching._

class PactSpec(config: PactConfiguration, pact: Pact)(implicit ec: ExecutionContext) extends FreeSpec with Assertions {
  s"provider ${pact.provider.name}" - {
    pact.interactions.toList.map { interaction =>
      val setup = config.setupHook(pact.provider)
      val service = config.service(pact.provider)
      s"in state: ${interaction.providerState}" -  {
        s"${interaction.description}" in {
          val response = for {
            inState <- setup.setup(interaction.providerState)
            response <- service.invoke(interaction.request)
          } yield response

          //TODO: configurable test timeout
          val actualResponse = Await.result(response, Duration.Inf)
          assert(ResponseMatching.matchRules(interaction.response, actualResponse) == MatchFound)
        }
      }
    }
  }
}
