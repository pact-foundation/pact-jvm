package au.com.dius.pact.provider

case class Address(protocol: String, host: String, port: Int = 80, path: String = "") {
  def url: String = s"$protocol$host:$port$path"
}

case class PactConfiguration(
  providerRoot: Address,
  stateChangeUrl: Option[Address])
