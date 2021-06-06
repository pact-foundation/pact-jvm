package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.MessageAndMetadata;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.MessageTarget;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.target.Target;
import au.com.dius.pact.provider.junitsupport.target.TestTarget;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@RunWith(PactRunner.class)
@Provider("test_provider_combined")
@PactFolder("pacts")
public class V4CombinedTest {
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(options().port(8888), false);

  @TestTarget
  public final Target httpTarget = new HttpTarget(8888);

  @TestTarget
  public final Target messageTarget = new MessageTarget();

  @Before
  public void before() {
    wireMockRule.stubFor(
      get(urlPathEqualTo("/data"))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody("{}")
          .withHeader("X-Ticket-ID", "1234")
          .withHeader("Content-Type", "application/json")
        )
    );
  }

  @State("message exists")
  public void messageExits() {

  }

  @PactVerifyProvider("Test Message")
  public MessageAndMetadata message() {
    return new MessageAndMetadata("{\"a\": \"1234-1234\"}".getBytes(), Map.of("destination", "a/b/c"));
  }
}
