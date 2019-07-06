package au.com.dius.pact.provider.junit

import au.com.dius.pact.provider.junit.loader.PactFolder
import au.com.dius.pact.provider.junit.target.HttpTarget
import au.com.dius.pact.provider.junit.target.TestTarget
import com.github.restdriver.clientdriver.ClientDriverRule
import com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse
import com.github.restdriver.clientdriver.RestClientDriver.onRequestTo
import mu.KLogging
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.runner.RunWith

@RunWith(PactRunner::class)
@Provider("providerMultipleStates")
@PactFolder("pacts")
class MultipleStatesContractTest {
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

  @State("state 1")
  fun state1() {
    // Prepare service before interaction that require "default" state
    // ...
    logger.info("Now service in state1")
    executedStates.add("state 1")
  }

  @State("state 2")
  fun toSecondState(params: Map<*, *>) {
    // Prepare service before interaction that require "state 2" state
    // ...
    logger.info("Now service in 'state 2' state: $params")
    executedStates.add("state 2")
  }

  @State("a gateway account with external id exists")
  fun gatewayAccount(params: Map<*, *>) {
    logger.info("Now service in 'gateway account' state: $params")
    executedStates.add("a gateway account with external id exists")
  }

  @State("a confirmed mandate exists")
  fun confirmedMandate(params: Map<*, *>) {
    logger.info("Now service in 'confirmed mandate' state: $params")
    executedStates.add("a confirmed mandate exists")
  }

  @State("something else exists")
  fun somethingElse() {
    logger.info("Now service in 'somethingElse' state")
    executedStates.add("something else exists")
  }

  @State("state 1", action = StateChangeAction.TEARDOWN)
  fun state1Teardown() {
    // Prepare service before interaction that require "default" state
    // ...
    logger.info("Now service in state1 Teardown")
    executedStates.add("state 1 Teardown")
  }

  @State("state 2", action = StateChangeAction.TEARDOWN)
  fun toSecondStateTeardown(params: Map<*, *>) {
    // Prepare service before interaction that require "state 2" state
    // ...
    logger.info("Now service in 'state 2' Teardown state: $params")
    executedStates.add("state 2 Teardown")
  }

  @State("a gateway account with external id exists", action = StateChangeAction.TEARDOWN)
  fun gatewayAccountTeardown(params: Map<*, *>) {
    logger.info("Now service in 'gateway account' Teardown state: $params")
    executedStates.add("a gateway account with external id exists Teardown")
  }

  @State("a confirmed mandate exists", action = StateChangeAction.TEARDOWN)
  fun confirmedMandateTeardown(params: Map<*, *>) {
    logger.info("Now service in 'confirmed mandate' Teardown state: $params")
    executedStates.add("a confirmed mandate exists Teardown")
  }

  @State("something else exists", action = StateChangeAction.TEARDOWN)
  fun somethingElseTeardown() {
    logger.info("Now service in 'somethingElse' Teardown state")
    executedStates.add("something else exists Teardown")
  }

  companion object: KLogging() {
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
      assertThat(executedStates, `is`(equalTo(listOf("state 1", "state 2", "a gateway account with external id exists",
        "a confirmed mandate exists", "something else exists", "something else exists Teardown",
        "a confirmed mandate exists Teardown", "a gateway account with external id exists Teardown",
        "state 2 Teardown", "state 1 Teardown"))))
    }
  }
}
