package com.dius.pact.runner

import com.dius.pact.model.{Interaction, Pact}
import scala.concurrent.{Await, ExecutionContext}
import org.scalatest.{Sequential, FreeSpec}
import scala.concurrent.duration.Duration

class InteractionSpec(pact:Pact, setup: SetupHook, service:Service, interaction:Interaction)(implicit ec:ExecutionContext) extends FreeSpec {
  //TODO: improve reporting to group by provider state
  s"provider ${pact.provider.name}" - {
    s"in state: ${interaction.providerState}" -  {
      s"${interaction.description}" in {
        //TODO: setup configured test timeout
        assert(Await.result(setup.setup(interaction.providerState), Duration.Inf), s"server failed to enter state: ${interaction.providerState}")
        //TODO: setup configured test timeout
        val result = Await.result(service.invoke(interaction.request), Duration.Inf)
        assert(result == interaction.response)
      }
    }
  }
}

class PactSpec(config: PactConfiguration, pact:Pact)(implicit ec:ExecutionContext) extends Sequential(
  pact.interactions.map{ i =>
    new InteractionSpec(pact, config.setupHook(pact.provider), config.service(pact.provider), i)
  } :_*
)
