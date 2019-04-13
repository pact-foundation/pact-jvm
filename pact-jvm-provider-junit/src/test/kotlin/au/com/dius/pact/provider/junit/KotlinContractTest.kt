package au.com.dius.pact.provider.junit

import au.com.dius.pact.provider.junit.loader.PactFolder
import au.com.dius.pact.provider.junit.target.HttpTarget
import au.com.dius.pact.provider.junit.target.TestTarget
import com.github.restdriver.clientdriver.ClientDriverRule
import com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse
import com.github.restdriver.clientdriver.RestClientDriver.onRequestTo
import org.apache.http.HttpRequest
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory

@RunWith(PactRunner::class)
@Provider("myAwesomeService")
@PactFolder("pacts")
class KotlinContractTest {
  @TestTarget
  val target = HttpTarget(port = 8332)

  @Before
  fun before() {
    // Rest data
    // Mock dependent service responses
    // ...
    embeddedService.addExpectation(
      onRequestTo("/data").withAnyParams(), giveEmptyResponse()
    )
  }

  @State("default")
  fun toDefaultState() {
    // Prepare service before interaction that require "default" state
    // ...
    LOGGER.info("Now service in default state")
  }

  @State("state 2")
  fun toSecondState(params: Map<*, *>) {
    // Prepare service before interaction that require "state 2" state
    // ...
    LOGGER.info("Now service in 'state 2' state: $params")
  }

  @TargetRequestFilter
  fun exampleRequestFilter(request: HttpRequest) {
    LOGGER.info("exampleRequestFilter called: $request")
  }

  companion object {
    // NOTE: this is just an example of embedded service that listens to requests, you should start here real service
    @ClassRule
    @JvmField
    val embeddedService = ClientDriverRule(8332)
    private val LOGGER = LoggerFactory.getLogger(KotlinContractTest::class.java)

    @BeforeClass
    fun setUpService() {
      // Run DB, create schema
      // Run service
      // ...
    }
  }
}
