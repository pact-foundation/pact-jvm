package au.com.dius.pact.provider.junit5

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.provider.junit.Provider
import au.com.dius.pact.provider.junit.State
import au.com.dius.pact.provider.junit.StateChangeAction
import au.com.dius.pact.provider.junit.loader.PactFolder
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import mu.KLogging
import org.apache.http.HttpRequest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver
import java.lang.String.format
import java.net.MalformedURLException
import java.net.URL

@Provider("providerMultipleStates")
@PactFolder("pacts")
@ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
class MultipleStatesContractTest {
  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider::class)
  internal fun testTemplate(pact: Pact<*>, interaction: Interaction, request: HttpRequest, context: PactVerificationContext) {
    logger.info("testTemplate called: " + pact.provider.name + ", " + interaction.description)
    context.verifyInteraction()
  }

  @BeforeEach
  @Throws(MalformedURLException::class)
  internal fun before(context: PactVerificationContext, @WiremockResolver.Wiremock server: WireMockServer,
                      @WiremockUriResolver.WiremockUri uri: String) {
    // Rest data
    // Mock dependent service responses
    // ...
    logger.info("BeforeEach - $uri")

    context.target = HttpTestTarget.fromUrl(URL(uri))

    server.stubFor(
      get(urlPathEqualTo("/data"))
        .willReturn(aResponse()
          .withStatus(204)
          .withHeader("Location", format("http://localhost:%s/ticket/%s", server.port(), "1234")
          )
          .withHeader("X-Ticket-ID", "1234"))
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
    val executedStates = mutableListOf<String>()

    @BeforeAll
    @JvmStatic
    fun beforeTest() {
      executedStates.clear()
    }

    @AfterAll
    @JvmStatic
    fun afterTest() {
      assertThat(executedStates, `is`(equalTo(listOf("state 1", "state 2", "a gateway account with external id exists",
        "a confirmed mandate exists", "something else exists", "state 1 Teardown", "state 2 Teardown",
        "a gateway account with external id exists Teardown", "a confirmed mandate exists Teardown",
        "something else exists Teardown"))))
    }
  }
}
