package au.com.dius.pact.provider.spring.spring7;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("Animal Profile Service")
@PactBroker
@IgnoreNoPactsToVerify(ignoreIoErrors = "true")
class ConsumerVersionSelectorJavaTest {
  static boolean called = false;

  @PactBrokerConsumerVersionSelectors
  public static SelectorBuilder consumerVersionSelectors() {
    called = true;
    return new SelectorBuilder().branch("current");
  }

  @TestTemplate
  @ExtendWith(PactVerificationSpring7Provider.class)
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
