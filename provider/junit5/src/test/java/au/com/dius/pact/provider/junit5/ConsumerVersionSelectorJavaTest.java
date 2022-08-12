package au.com.dius.pact.provider.junit5;

import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Provider("Animal Profile Service")
@PactBroker(url = "http://broker.host")
@IgnoreNoPactsToVerify(ignoreIoErrors = "true")
class ConsumerVersionSelectorJavaTest {
  static boolean called = false;

  @PactBrokerConsumerVersionSelectors
  public static SelectorBuilder consumerVersionSelectors() {
    called = true;
    return new SelectorBuilder().branch("current");
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void pactVerificationTestTemplate(PactVerificationContext context) {
    if (context != null) {
      context.verifyInteraction();
    }
  }

  @AfterAll
  static void after() {
    assertThat("consumerVersionSelectors() was not called", called, is(true));
  }
}
