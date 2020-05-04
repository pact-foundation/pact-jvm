package au.com.dius.pact.provider.junit.filter

import au.com.dius.pact.provider.junit.PactRunner
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.StateChangeAction
import au.com.dius.pact.provider.junitsupport.loader.PactFilter
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import au.com.dius.pact.provider.junit.target.HttpTarget
import au.com.dius.pact.provider.junitsupport.target.TestTarget
import au.com.dius.pact.provider.junitsupport.filter.InteractionFilter
import com.github.restdriver.clientdriver.ClientDriverRule
import com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse
import com.github.restdriver.clientdriver.RestClientDriver.onRequestTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.runner.RunWith

@RunWith(PactRunner::class)
@Provider("providerWithMultipleInteractions")
@PactFolder("pacts")
@PactFilter("^\\/data.*", filter = InteractionFilter.ByRequestPath::class)
class FilterByRequestPathTest {
  @TestTarget
  val target = HttpTarget(port = 8332)

  @Before
  fun before() {
    embeddedService.addExpectation(
      onRequestTo("/data").withAnyParams(), giveEmptyResponse()
    )
  }

  @State("state1")
  fun state1() {
    executedStates.add("state1")
  }

  @State("state1", action = StateChangeAction.TEARDOWN)
  fun state1Teardown() {
    executedStates.add("state1 Teardown")
  }

  companion object {
    @ClassRule
    @JvmField
    val embeddedService = ClientDriverRule(8332)

    val executedStates = mutableListOf<String>()

    @BeforeClass
    @JvmStatic
    fun beforeTest() {
      executedStates.clear()
    }

    @AfterClass
    @JvmStatic
    fun afterTest() {
      assertThat(executedStates, `is`(equalTo(listOf("state1", "state1 Teardown"))))
    }
  }
}
