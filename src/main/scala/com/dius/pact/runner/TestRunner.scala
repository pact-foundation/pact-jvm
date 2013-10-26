package com.dius.pact.runner

import com.dius.pact.model.Interaction
import scala.concurrent.{ExecutionContext, Future}

class TestRunner(setupHook: SetupHook, service: Service)(implicit executionContext: ExecutionContext) {
  def run(interaction:Interaction):Future[Boolean] = {
    setupHook.setup(interaction.providerState)
    val result = service.invoke(interaction.request)
    result.map( _ == interaction.response)
  }
}
