package au.com.dius.pact.provider.junit5;

import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@Provider("Animal Profile Service")
@PactBroker(url = "http://broker.host")
@IgnoreNoPactsToVerify(ignoreIoErrors = "true")
class NoPactsToVerifyParameterResolverTest {

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void pactVerificationTestTemplateWithClassicHttpRequest(ClassicHttpRequest request, PactVerificationContext context) {
    assertThat(request, is(nullValue()));
    if (context != null) {
      context.verifyInteraction();
    }
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void pactVerificationTestTemplateWithHttpRequest(HttpRequest request, PactVerificationContext context) {
    assertThat(request, is(nullValue()));
    if (context != null) {
      context.verifyInteraction();
    }
  }

}
