package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
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
@PactTestFor(providerName = "ip6_provider", pactVersion = PactSpecVersion.V3)
@MockServerConfig(hostInterface = "::1", port = "1234")
public class Ip6Test {
  @Pact(consumer = "ApiConsumer")
  public RequestResponsePact articles(PactDslWithProvider builder) {
    return builder
            .uponReceiving("GET request")
                    .path("/test")
                    .method("GET")
                    .willRespondWith()
                    .status(200)
                    .toPact();
  }

  @Test
  @PactTestFor
  void testApi(MockServer mockServer) throws IOException {
    ClassicHttpResponse httpResponse = (ClassicHttpResponse) Request.get(mockServer.getUrl() + "/test").execute().returnResponse();
    assertThat(httpResponse.getCode(), is(equalTo(200)));
  }
}
