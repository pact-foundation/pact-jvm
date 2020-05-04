package au.com.dius.pact.provider.scalatest

import java.net.URL

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.PactSpecVersion
import io.netty.channel.ChannelHandler.Sharable
import unfiltered.netty.cycle.Plan.Intent
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

  @Sharable
  class TestPlan extends Plan with SynchronousExecution with ServerErrorResponse {
    def intent: Intent = {
      case req => {
        ResponseString(s"""["All Done $state"]""")
      }
    }
  }

  def startServer(): Unit = {

    val config = MockProviderConfig.createDefault(PactSpecVersion.V3)
    val plan = new TestPlan
    val server = Server.http(config.getPort, config.getHostname).handler(plan)

    this.url = new URL(s"http://${config.getHostname}:${config.getPort}")
    this.server = server.start()
  }

  def stopServer() = server.stop()

}
