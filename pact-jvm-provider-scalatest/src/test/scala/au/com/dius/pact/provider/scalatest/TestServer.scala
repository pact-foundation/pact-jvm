package au.com.dius.pact.provider.scalatest

import java.net.URL

import au.com.dius.pact.model.{MockProviderConfig, PactSpecVersion}
import unfiltered.netty.cycle.{Plan, SynchronousExecution}
import unfiltered.netty.{Server, ServerErrorResponse}
import unfiltered.response.ResponseString

/**
  * This is not really part of the example, it's just a fake server instead of building a real provider
  */
class TestServer {

  private var server: Server = _
  var url: URL = _
  var state: String = _

  def startServer(): Unit = {

    val config = MockProviderConfig.createDefault(PactSpecVersion.V3)
    val server = Server.http(config.port, config.hostname).handler(new Plan with SynchronousExecution with ServerErrorResponse {
      def intent: Plan.Intent = {
        case req => {
          ResponseString(s"""["All Done $state"]""")
        }
      }
    })

    this.url = new URL(s"http://${config.hostname}:${config.port}")
    this.server = server.start()
  }

  def stopServer() = server.stop()


}