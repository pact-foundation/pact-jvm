package au.com.dius.pact.provider.spring.junit5;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

/*
  This is a test for issue https://github.com/pact-foundation/pact-jvm/issues/1242
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("Foo Provider")
@PactBroker(scheme="https", host = "test.pactflow.io", port = "443", authentication = @PactBrokerAuth(token = "anyToken"))
@IgnoreNoPactsToVerify(ignoreIoErrors = "true")
class FooControllerTest {

  @LocalServerPort
  int port;

  @BeforeEach
  void setup(PactVerificationContext context) {
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void pactVerificationTestTemplate(PactVerificationContext context) {
  }
}
