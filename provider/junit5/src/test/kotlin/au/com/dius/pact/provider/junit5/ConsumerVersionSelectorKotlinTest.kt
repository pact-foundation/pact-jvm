package au.com.dius.pact.provider.junit5

import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith

@Provider("Animal Profile Service")
@PactBroker(url = "http://broker.host")
@IgnoreNoPactsToVerify(ignoreIoErrors = "true")
class ConsumerVersionSelectorKotlinTest {
  @PactBrokerConsumerVersionSelectors
  fun consumerVersionSelectors(): SelectorBuilder {
    called = true
    return SelectorBuilder().branch("current")
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider::class)
  fun pactVerificationTestTemplate(context: PactVerificationContext?) {
    context?.verifyInteraction()
  }

  companion object {
    private var called: Boolean = false

    @AfterAll
    fun after() {
      MatcherAssert.assertThat("consumerVersionSelectors() was not called", called, Matchers.`is`(true))
    }
  }
}

@PactBroker(url = "http://broker.host")
@IgnoreNoPactsToVerify(ignoreIoErrors = "true")
abstract class Base {
  @PactBrokerConsumerVersionSelectors
  fun consumerVersionSelectors(): SelectorBuilder {
    called = true
    return SelectorBuilder().branch("current")
  }

  companion object {
    var called: Boolean = false
  }
}

@Provider("Animal Profile Service")
class ConsumerVersionSelectorKotlinTestWithAbstractBase: Base() {
  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider::class)
  fun pactVerificationTestTemplate(context: PactVerificationContext?) {
    context?.verifyInteraction()
  }

  companion object {
    @AfterAll
    fun after() {
      MatcherAssert.assertThat("consumerVersionSelectors() was not called", called, Matchers.`is`(true))
    }
  }
}

@Provider("Animal Profile Service")
@PactBroker(url = "http://broker.host")
@IgnoreNoPactsToVerify(ignoreIoErrors = "true")
class ConsumerVersionSelectorKotlinTestWithCompanionMethod {
  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider::class)
  fun pactVerificationTestTemplate(context: PactVerificationContext?) {
    context?.verifyInteraction()
  }

  companion object {
    private var called: Boolean = false

    @PactBrokerConsumerVersionSelectors
    fun consumerVersionSelectors(): SelectorBuilder {
      called = true
      return SelectorBuilder().branch("current")
    }

    @AfterAll
    fun after() {
      MatcherAssert.assertThat("consumerVersionSelectors() was not called", called, Matchers.`is`(true))
    }
  }
}
