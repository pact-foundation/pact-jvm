package com.dius.pact.author

case class PactServerConfig(port: Int = 9090, interface: String = "localhost") {
  def url: String = s"http://$interface:$port"
}