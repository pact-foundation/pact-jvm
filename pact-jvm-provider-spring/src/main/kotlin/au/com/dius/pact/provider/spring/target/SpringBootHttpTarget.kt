package au.com.dius.pact.provider.spring.target

import au.com.dius.pact.provider.junit.target.HttpTarget

/**
 * This class sets up an HTTP target configured with the springboot application. Basically, it allows the port
 * to be overridden by the interaction runner which looks up the server
 * port from the spring context.
 */
class SpringBootHttpTarget : HttpTarget("localhost", 0) {
  fun setPort(port: Int) {
    super.port = port
  }
}
