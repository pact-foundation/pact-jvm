package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.target.Target;
import au.com.dius.pact.provider.junitsupport.target.TestTarget;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@RunWith(PactRunner.class)
@Provider("V4Service")
@PactFolder("pacts")
public class V4StatusCodeMatcherTest {
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(options().port(8888), false);

  @TestTarget
  public final Target httpTarget = new HttpTarget(8888);

  @Before
  public void before() {
    wireMockRule.stubFor(
      get(urlPathEqualTo("/test"))
        .willReturn(aResponse().withStatus(204))
    );
    wireMockRule.stubFor(
      get(urlPathEqualTo("/test2"))
        .willReturn(aResponse().withStatus(404))
    );
  }
}
