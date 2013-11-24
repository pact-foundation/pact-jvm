package com.dius.pact.author

import scala.concurrent.ExecutionContext

object Config {
  val port = 9090
  val interface = "localhost"
  def url = s"http://$interface:$port"
  implicit val executionContext = ExecutionContext.Implicits.global
}
