package com.dius.pact.runner

import scala.concurrent.Future

trait SetupHook {
  def setup(setupIdentifier : String) : Future[Boolean]
}
