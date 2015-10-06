package au.com.dius.pact.model

import java.net.ServerSocket
import scala.util.control.NonFatal

case class MockProviderConfig(port: Int, hostname: String, pactConfig: PactConfig) {
  def url: String = s"http://$hostname:$port"
}

object MockProviderConfig {
  val portLowerBound = 20000
  val portUpperBound = 40000

  def createDefault() : MockProviderConfig = createDefault("localhost", PactConfig(PactSpecVersion.V2))
  def createDefault(pactConfig: PactConfig) : MockProviderConfig = createDefault("localhost", pactConfig)
  def createDefault(host: String, pactConfig: PactConfig) =
    MockProviderConfig(randomPort(portLowerBound, portUpperBound).get, host, pactConfig)
  def create(lower: Int, upper: Int, pactConfig: PactConfig) =
    MockProviderConfig(randomPort(lower, upper).get, "localhost", pactConfig)

  def randomPort(lower: Int, upper: Int) = {
    import util.Random.nextInt
    Stream.continually(nextInt(upper - lower)).map(_ + lower).find(portAvailable)
  }

  private def portAvailable(p: Int):Boolean = {
    var socket: ServerSocket = null
    try {
      socket = new ServerSocket(p)
      true
    } catch {
      case NonFatal(_) => false
    } finally {
      if (socket != null)
        try {
          socket.close()
        } catch {
          case NonFatal(_) =>
        }
    }
  }
}
