package au.com.dius.pact.server

import au.com.dius.pact.model._
import ch.qos.logback.classic.Level
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions

object ListServers {

  def apply(oldState: ServerState): Result = {
    val body = OptionalBody.body(("{\"ports\": [" + oldState.keySet.mkString(", ") + "]}").getBytes)
    Result(new Response(200, JavaConversions.mapAsJavaMap(Map("Content-Type" -> "application/json")), body), oldState)
  }
}

case class Result(response: Response, newState: ServerState)

case class Config(port: Int = 29999,
                  host: String = "localhost",
                  daemon: Boolean = false,
                  portLowerBound: Int = 20000,
                  portUpperBound: Int = 40000,
                  debug: Boolean = false,
                  pactVersion: Int = 2,
                  keystorePath: String = "",
                  keystorePassword: String = "",
                  sslPort : Int = 8443)

object Server extends App {

  val parser = new scopt.OptionParser[Config]("pact-jvm-server") {
    arg[Int]("port") optional() action { (x, c) => c.copy(port = x) } text("port to run on (defaults to 29999)")
    help("help") text("prints this usage text")
    opt[String]('h', "host") action { (x, c) => c.copy(host = x) } text("host to bind to (defaults to localhost)")
    opt[Int]('l', "mock-port-lower") action { (x, c) => c.copy(portLowerBound = x) } text("lower bound to allocate mock ports (defaults to 20000)")
    opt[Int]('u', "mock-port-upper") action { (x, c) => c.copy(portUpperBound = x) } text("upper bound to allocate mock ports (defaults to 40000)")
    opt[Unit]('d', "daemon") action { (_, c) => c.copy(daemon = true) } text("run as a daemon process")
    opt[Unit]("debug") action { (_, c) => c.copy(debug = true) } text("run with debug logging")
    opt[Int]('v', "pact-version") action { (x, c) => c.copy(pactVersion = x) } text("pact version to generate for (2 or 3)")
    opt[String]('k', "keystore-path") action { (x, c) => c.copy(keystorePath = x) } text("Path to keystore")
    opt[String]('p', "keystore-password") action { (x, c) => c.copy(keystorePassword = x) } text("Keystore password")
    opt[Int]('s', "ssl-port") action { (x, c) => c.copy(sslPort = x) } text("Ssl port the mock server should run on. lower and upper bounds are ignored")
  }

  parser.parse(args, Config()) match {
    case Some(config) =>
      val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[ch.qos.logback.classic.Logger]
      if (config.debug) {
        logger.setLevel(Level.DEBUG)
      } else {
        logger.setLevel(Level.INFO)
      }
      val server = _root_.unfiltered.netty.Server.http(config.port, config.host)
        .handler(RequestHandler(new ServerStateStore(), config))

      if(!config.keystorePath.isEmpty) {
        println(s"Using keystore '${config.keystorePath}' for mock https server")
      }

      println(s"starting unfiltered app at ${config.host} on port ${config.port}")
      server.start()
      if (!config.daemon) {
        readLine("press enter to stop server:\n")
        server.stop()
      }

    case None =>
      parser.showUsage
  }
}
