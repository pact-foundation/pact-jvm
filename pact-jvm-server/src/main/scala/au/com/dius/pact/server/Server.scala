package au.com.dius.pact.server

import ch.qos.logback.classic.Level
import org.slf4j.{Logger, LoggerFactory}

object Server extends App {

  val parser = new scopt.OptionParser[Config]("pact-jvm-server") {
    arg[Int]("port") optional() action { (x, c) => c.copyPort(x) } text("port to run on (defaults to 29999)")
    help("help") text("prints this usage text")
    opt[String]('h', "host") action { (x, c) => c.copyHost(x) } text("host to bind to (defaults to localhost)")
    opt[Int]('l', "mock-port-lower") action { (x, c) => c.copyPortLowerBound(x) } text("lower bound to allocate mock ports (defaults to 20000)")
    opt[Int]('u', "mock-port-upper") action { (x, c) => c.copyPortUpperBound(x) } text("upper bound to allocate mock ports (defaults to 40000)")
    opt[Unit]('d', "daemon") action { (_, c) => c.copyDaemon(true) } text("run as a daemon process")
    opt[Unit]("debug") action { (_, c) => c.copyDebug(true) } text("run with debug logging")
    opt[Int]('v', "pact-version") action { (x, c) => c.copyPactVersion(x) } text("pact version to generate for (2 or 3)")
    opt[String]('k', "keystore-path") action { (x, c) => c.copyKeystorePath(x) } text("Path to keystore")
    opt[String]('p', "keystore-password") action { (x, c) => c.copyKeystorePassword(x) } text("Keystore password")
    opt[Int]('s', "ssl-port") action { (x, c) => c.copySslPort(x) } text("Ssl port the mock server should run on. lower and upper bounds are ignored")
    opt[String]('b', "broker") action {(x, c) => c.copyBroker(x)} text("URL of broker where to publish contracts to")
    opt[String]('t', "token") action {(x, c) => c.copyAuthToken(x)} text("Auth token for publishing the pact to broker")
  }

  parser.parse(args, new Config()) match {
    case Some(config) =>
      val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[ch.qos.logback.classic.Logger]
      if (config.getDebug) {
        logger.setLevel(Level.DEBUG)
      } else {
        logger.setLevel(Level.INFO)
      }
      val server = _root_.unfiltered.netty.Server.http(config.getPort, config.getHost)
        .handler(RequestHandler(new ServerStateStore(), config))

      if (config.getKeystorePath.nonEmpty) {
        println(s"Using keystore '${config.getKeystorePath}' for mock https server")
      }

      println(s"starting unfiltered app at ${config.getHost} on port ${config.getPort}")
      server.start()
      if (!config.getDaemon) {
        readLine("press enter to stop server:\n")
        server.stop()
      }

    case None =>
      parser.showUsage
  }
}
