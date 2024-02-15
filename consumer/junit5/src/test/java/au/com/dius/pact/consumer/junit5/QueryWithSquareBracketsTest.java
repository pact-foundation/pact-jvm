package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "QueryWithSquareBrackets", pactVersion = PactSpecVersion.V2)
@Disabled
public class QueryWithSquareBracketsTest {

  @Pact(consumer="test_consumer")
  public RequestResponsePact pact1(PactDslWithProvider builder) {
    return builder
      .uponReceiving("Get request with square brackets")
      .path("/")
      .method("GET")
      .matchQuery("principle_identifier[account_id]", "\\d{4}", "1234")
      .matchQuery("identifier[other]", "\\w{3}\\d{4}", "ABC1234")
      .willRespondWith()
      .status(200)
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "pact1")
  void runTest1(MockServer mockServer) throws IOException {
    String url = mockServer.getUrl() + "?principle_identifier[account_id]=1122&identifier[other]=AAa0000";
    ClassicHttpResponse httpResponse = (ClassicHttpResponse) Request.get(url)
            .execute().returnResponse();
    assertThat(httpResponse.getCode(), is(200));
  }

  @Pact(consumer="test_consumer")
  public RequestResponsePact pact2(PactDslWithProvider builder) {
    return builder
      .uponReceiving("Put request with square brackets")
      .path("/")
      .method("PUT")
      .matchQuery("principle_identifier[account_id]", "\\d{4}", "1234")
      .matchQuery("identifier[other]", "\\w{3}\\d{4}", "ABC1234")
      .willRespondWith()
      .status(200)
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "pact2")
  void runTest2(MockServer mockServer) throws IOException {
    String url = mockServer.getUrl() + "?principle_identifier[account_id]=1122&identifier[other]=AAa0000";
    ClassicHttpResponse httpResponse = (ClassicHttpResponse) Request.put(url)
            .execute().returnResponse();
    assertThat(httpResponse.getCode(), is(200));
  }
}
