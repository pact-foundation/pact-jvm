package au.com.dius.pact.model

import java.net.ServerSocket

import io.netty.handler.ssl.util.SelfSignedCertificate

import scala.util.control.NonFatal

trait MockProviderConfig {
  def port: Int
  def hostname: String
  def pactVersion: PactSpecVersion
  def url: String
}

case class MockHttpProviderConfig(port: Int, hostname: String, pactVersion: PactSpecVersion) extends MockProviderConfig {
  def url: String = s"http://$hostname:$port"
}

case class MockHttpsProviderConfig(port: Int, hostname: String, pactVersion: PactSpecVersion) extends MockProviderConfig {
  def httpsCertificate: SelfSignedCertificate = new SelfSignedCertificate()

  def url: String = s"https://$hostname:$port"
}

object MockProviderConfig {
  val portLowerBound = 20000
  val portUpperBound = 40000

  def createDefault() : MockProviderConfig = createDefault("localhost", PactSpecVersion.V3)
  def createDefault(pactVersion: PactSpecVersion) : MockProviderConfig = createDefault("localhost", pactVersion)
  def createDefault(host: String, pactVersion: PactSpecVersion) =
    MockHttpProviderConfig(randomPort(portLowerBound, portUpperBound).get, host, pactVersion)
  def create(lower: Int, upper: Int, pactVersion: PactSpecVersion) =
    MockHttpProviderConfig(randomPort(lower, upper).get, "localhost", pactVersion)
  def apply(port: Int, hostname: String, pactVersion: PactSpecVersion) = MockHttpProviderConfig(port, hostname, pactVersion)

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
