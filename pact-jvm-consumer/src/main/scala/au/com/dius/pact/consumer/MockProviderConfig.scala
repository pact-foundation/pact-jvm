package au.com.dius.pact.consumer

import java.net.ServerSocket
import scala.util.control.NonFatal

case class MockProviderConfig(port: Int, interface: String) {
  def url: String = s"http://$interface:$port"
}

object MockProviderConfig {
  val portLowerBound = 20000
  val portUpperBound = 40000

  def createDefault() = MockProviderConfig(randomPort.get, "localhost")
  
  def randomPort = {
    import util.Random.nextInt
    Stream.continually(nextInt(portUpperBound - portLowerBound)).map(_ + portLowerBound).find(portAvailable)
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
