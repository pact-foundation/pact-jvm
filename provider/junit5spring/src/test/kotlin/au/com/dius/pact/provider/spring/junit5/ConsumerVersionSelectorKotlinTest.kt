package au.com.dius.pact.provider.spring.junit5

import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("Animal Profile Service")
@PactBroker
@IgnoreNoPactsToVerify(ignoreIoErrors = "true")
@Disabled // TODO: this fails with NoClassDefFoundError: org/slf4j/impl/StaticLoggerBinder
open class ConsumerVersionSelectorKotlinTest {
  @PactBrokerConsumerVersionSelectors
  fun consumerVersionSelectors(): SelectorBuilder {
    called = true
    return SelectorBuilder().branch("current")
  }

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider::class)
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
