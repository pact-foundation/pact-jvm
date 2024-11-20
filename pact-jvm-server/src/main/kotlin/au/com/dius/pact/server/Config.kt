package au.com.dius.pact.server

data class Config @JvmOverloads constructor(
  val port: Int = 29999,
  val host: String = "localhost",
  val daemon: Boolean = false,
  val portLowerBound: Int = 20000,
  val portUpperBound: Int = 40000,
  val debug: Boolean = false,
  val pactVersion: Int = 2,
  val keystorePath: String = "",
  val keystorePassword: String = "",
  val sslPort : Int = 8443,
  val broker: String = "",
  val authToken: String = ""
) {
  // Scala/Groovy can't access the copy method correctly
  fun copyPort(port: Int) = this.copy(port = port)
  fun copyHost(host: String) = this.copy(host = host)
  fun copyDaemon(daemon: Boolean) = this.copy(daemon = daemon)
  fun copyPortLowerBound(portLowerBound: Int) = this.copy(portLowerBound = portLowerBound)
  fun copyPortUpperBound(portUpperBound: Int) = this.copy(portUpperBound = portUpperBound)
  fun copyDebug(debug: Boolean) = this.copy(debug = debug)
  fun copyPactVersion(pactVersion: Int) = this.copy(pactVersion = pactVersion)
  fun copyKeystorePath(keystorePath: String) = this.copy(keystorePath = keystorePath)
  fun copyKeystorePassword(keystorePassword: String) = this.copy(keystorePassword = keystorePassword)
  fun copySslPort(sslPort : Int) = this.copy(sslPort = sslPort)
  fun copyBroker(broker: String) = this.copy(broker = broker)
  fun copyAuthToken(authToken: String) = this.copy(authToken = authToken)
}
