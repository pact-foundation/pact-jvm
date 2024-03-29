package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonArrayMinLike;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "TextProvider", pactVersion = PactSpecVersion.V3)
public class Defect1579Test {
  @Pact(consumer = "TextConsumer")
  public RequestResponsePact articles(PactDslWithProvider builder) {
    return builder
      .given("A text generation job finished successfully")
      .uponReceiving("A request to download text")
      .pathFromProviderState("/textresult/${jobId}", "/textresult/dummyJobId")
      .method("GET")
      .willRespondWith()
      .status(200)
      .headers(Map.of("Content-Type", "text/plain"))
      .body(PactDslRootValue.stringMatcher("^.+$", "whatever"))
      .toPact();
  }

  @Test
  @PactTestFor
  void testApi(MockServer mockServer) throws IOException {
    String response = Request.get(mockServer.getUrl() + "/textresult/dummyJobId")
      .execute().returnContent().asString();
    assertThat(response, is(equalTo("whatever")));
  }
}
