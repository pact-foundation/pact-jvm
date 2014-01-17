package au.com.dius.pact.consumer

case class PactServerConfig(port: Int = 9090, interface: String = "localhost") {
  def url: String = s"http://$interface:$port"
}