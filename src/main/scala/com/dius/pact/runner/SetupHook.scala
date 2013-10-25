package com.dius.pact.runner

trait SetupHook {
  def setup(setupIdentifier : String) : Boolean
}
