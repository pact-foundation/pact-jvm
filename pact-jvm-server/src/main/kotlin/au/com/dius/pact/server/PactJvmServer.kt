package au.com.dius.pact.server

import au.com.dius.pact.core.support.isNotEmpty
import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PactJvmServer: CliktCommand() {
  val port: Int? by argument(help = "port to run on (defaults to 29999)").int().optional()
  val host by option("-h", "--host", help = "host to bind to (defaults to localhost)")
  val portLowerBound: Int? by option("-l", "--mock-port-lower", help = "lower bound to allocate mock ports (defaults to 20000)").int()
  val portUpperBound: Int? by option("-u", "--mock-port-upper", help = "upper bound to allocate mock ports (defaults to 40000)").int()
  val daemon by option("--daemon", "-d", help = "run as a daemon process").flag(default = false)
  val debug by option("--debug", help = "run with debug logging").flag(default = false)
  val pactVersion: Int? by option("-v", "--pact-version", help = "pact version to generate for (2 or 3)").int()
  val keystorePath by option("-k", "--keystore-path", help = "Path to keystore")
  val keystorePassword by option("-p", "--keystore-password", help = "Keystore password")
  val sslPort: Int? by option("-s", "--ssl-port", help = "Ssl port the mock server should run on. lower and upper bounds are ignored").int()
  val brokerUrl by option("-b", "--broker", help = "URL of broker where to publish contracts to")
  val brokerToken by option("-t", "--token", help = "Auth token for publishing the pact to broker")

  override fun run() {
    var config = Config(daemon = daemon, debug = debug)
    if (port != null) config = config.copy(port = port!!)
    if (host != null) config = config.copy(host = host!!)
    if (portLowerBound != null) config = config.copy(portLowerBound = portLowerBound!!)
    if (portUpperBound != null) config = config.copy(portUpperBound = portUpperBound!!)
    if (pactVersion != null) config = config.copy(pactVersion = pactVersion!!)
    if (keystorePath != null) config = config.copy(keystorePath = keystorePath!!)
    if (keystorePassword != null) config = config.copy(keystorePassword = keystorePassword!!)
    if (sslPort != null) config = config.copy(sslPort = sslPort!!)
    if (brokerUrl != null) config = config.copy(broker = brokerUrl!!)
    if (brokerToken != null) config = config.copy(authToken = brokerToken!!)

    val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
    if (debug) {
      logger.setLevel(Level.DEBUG)
    } else {
      logger.setLevel(Level.INFO)
    }

    val mainServer = MainServer(ServerStateStore(), config)

    if (keystorePath.isNotEmpty()) {
      echo("Using keystore '${keystorePath}' for mock https server")
    }

    echo("starting main server at ${config.host} on port ${config.port}")
    if (!config.daemon) {
      mainServer.server.start(false)
      echo("press enter to stop server:\n")
      readLine()
      mainServer.server.stop(100, 1000)
    } else {
      mainServer.server.start(true)
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) = PactJvmServer().main(args)
  }
}
