package com.dius.pact.runner

import com.dius.pact.model.{Interaction, Pact}
import scala.concurrent.ExecutionContext

case class PactRunner(config: PactConfiguration)(implicit ec:ExecutionContext) {
  def run(pact:Pact) = {
    val step = runInteraction(config.setupHook(pact.provider), config.service(pact.provider))
    pact.interactions.map(step)
  }

  def runInteraction(setup:HttpSetupHook, service:Service) = (interaction:Interaction) => {
    setup.setup(interaction.providerState)
    val result = service.invoke(interaction.request)
    result.map( _ == interaction.response)
  }
}
