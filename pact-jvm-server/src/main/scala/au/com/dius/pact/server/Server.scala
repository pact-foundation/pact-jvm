package au.com.dius.pact.server

import au.com.dius.pact.model._
import ch.qos.logback.classic.Level
import org.json4s._
import org.json4s.jackson.Serialization
import org.slf4j.LoggerFactory
import org.slf4j.Logger

object ListServers {

  def apply(oldState: ServerState): Result = {
    implicit val formats = Serialization.formats(NoTypeHints)
    val body = Serialization.write(Map("ports" -> oldState.keySet))
    Result(Response(200, Map("Content-Type" -> "application/json"), body, null), oldState)
  }
}


case class Result(response: Response, newState: ServerState)

case class Config(port: Int = 29999, host: String = "localhost", daemon: Boolean = false,
                  portLowerBound: Int = 20000, portUpperBound: Int = 40000, debug: Boolean = false)

object Server extends App {

  val parser = new scopt.OptionParser[Config]("pact-jvm-server") {
    arg[Int]("port") optional() action { (x, c) => c.copy(port = x) } text("port to run on (defaults to 29999)")
    help("help") text("prints this usage text")
    opt[String]('h', "host") action { (x, c) => c.copy(host = x) } text("host to bind to (defaults to localhost)")
    opt[Int]('l', "mock-port-lower") action { (x, c) => c.copy(portLowerBound = x) } text("lower bound to allocate mock ports (defaults to 20000)")
    opt[Int]('u', "mock-port-upper") action { (x, c) => c.copy(portUpperBound = x) } text("upper bound to allocate mock ports (defaults to 40000)")
    opt[Unit]('d', "daemon") action { (_, c) => c.copy(daemon = true) } text("run as a daemon process")
    opt[Unit]("debug") action { (_, c) => c.copy(debug = true) } text("run with debug logging")
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
