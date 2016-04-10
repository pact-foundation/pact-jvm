package au.com.dius.pact.model

import java.net.ServerSocket
import scala.util.control.NonFatal

trait MockProviderConfig {
  def port: Int
  def hostname: String
  def pactConfig: PactConfig
  def url: String
}

case class MockHttpProviderConfig(port: Int, hostname: String, pactConfig: PactConfig) extends MockProviderConfig {
  def url: String = s"http://$hostname:$port"
}

case class MockHttpsProviderConfig(port: Int, hostname: String, pactConfig: PactConfig) extends MockProviderConfig {
  def url: String = s"https://$hostname:$port"
}

object MockProviderConfig {
  val portLowerBound = 20000
  val portUpperBound = 40000

  def createDefault() : MockProviderConfig = createDefault("localhost", PactConfig(PactSpecVersion.V2))
  def createDefault(pactConfig: PactConfig) : MockProviderConfig = createDefault("localhost", pactConfig)
  def createDefault(host: String, pactConfig: PactConfig) =
    MockHttpProviderConfig(randomPort(portLowerBound, portUpperBound).get, host, pactConfig)
  def create(lower: Int, upper: Int, pactConfig: PactConfig) =
    MockHttpProviderConfig(randomPort(lower, upper).get, "localhost", pactConfig)
  def apply(port: Int, hostname: String, pactConfig: PactConfig) = MockHttpProviderConfig(port, hostname, pactConfig)

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
