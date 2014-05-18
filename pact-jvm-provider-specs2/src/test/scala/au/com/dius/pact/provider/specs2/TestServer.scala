package au.com.dius.pact.provider.specs2

import unfiltered.netty.{ServerErrorResponse, Http}
import unfiltered.netty.cycle.{SynchronousExecution, Plan}
import unfiltered.response.ResponseString
import au.com.dius.pact.model.MockProviderConfig


/**
 * This is not really part of the example, it's just a fake server instead of building a real provider
 */
case class TestServer(state: String) {
  def run[T](code: String => T):T = {
    val config = MockProviderConfig.createDefault()
    val server = Http(config.port, config.interface).handler(new Plan with SynchronousExecution with ServerErrorResponse {
      def intent: Plan.Intent = {
        case req => {
          ResponseString("[\"All Done\"]")
        }
      }
    })

    server.start()
    try {
      code(config.url)
    } finally {
      server.stop()
    }
  }
}
