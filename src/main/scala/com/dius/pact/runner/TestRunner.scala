package com.dius.pact.runner

import com.dius.pact.model.Interaction

class TestRunner(setupHook: SetupHook, service: Service) {
  def run(interaction:Interaction):Boolean = {
    setupHook.setup(interaction.providerState)
    service.invoke(interaction.request)
    true
  }
}
