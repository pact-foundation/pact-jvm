package com.dius.pact.runner

import scala.concurrent.{Await, ExecutionContext}
import org.scalatest.{Assertions, Sequential, FreeSpec}
import scala.concurrent.duration.Duration
import com.dius.pact.model._
import com.dius.pact.model.Matching._

class PactSpec(config: PactConfiguration, pact: Pact)(implicit ec: ExecutionContext) extends FreeSpec with Assertions {
  s"pact for consumer ${pact.consumer.name} provider ${pact.provider.name}" - {
    pact.interactions.toList.map { interaction =>
      val setup = config.setupHook(pact.provider)
      val service = config.service(pact.provider)
      s"""interaction "${interaction.description}" in state: "${interaction.providerState}"""" in {
        val response = for {
          inState <- setup.setup(interaction.providerState)
          response <- service.invoke(interaction.request)
        } yield response

        //TODO: configurable test timeout
        val actualResponse = Await.result(response, Duration.Inf)
        try {
          assert(ResponseMatching.matchRules(interaction.response, actualResponse) == MatchFound)
        } catch {
          case t: Throwable => {
            t.printStackTrace()
            throw t
          }
        }
      }
    }
  }
}
