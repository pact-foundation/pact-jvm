package au.com.dius.pact.consumer.junit;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.hc.client5.http.fluent.Request;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@Ignore // Test is failing on Windows
public class IP6Test {

  @Rule
  public PactProviderRule provider = new PactProviderRule("ip6_provider", "::1", 0, this);

  @Pact(provider = "ip6_provider", consumer = "test_consumer")
  public RequestResponsePact getRequest(PactDslWithProvider builder) {
    return builder
      .uponReceiving("get request")
      .path("/path")
      .method("GET")
      .willRespondWith()
      .status(200)
      .body("{}", "application/json")
      .toPact();
  }

  @Test
  @PactVerification("ip6_provider")
  public void runTest() throws IOException {
    assertThat(Request.get(provider.getUrl() + "/path").execute().returnContent().asString(), is(equalTo("{}")));
  }
}
