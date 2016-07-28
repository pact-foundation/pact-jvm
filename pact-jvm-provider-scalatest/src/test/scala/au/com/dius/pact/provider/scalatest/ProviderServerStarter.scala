package au.com.dius.pact.provider.scalatest

import java.net.URL

class ProviderServerStarter extends ServerStarter {
  var server: TestServer = _

  override def startServer(): URL = {
    server = new TestServer()
    server.startServer()
    server.url
  }

  override def initState(state: String): Unit = server.state = state

  override def stopServer(): Unit = {
    server.stopServer()
  }
}
