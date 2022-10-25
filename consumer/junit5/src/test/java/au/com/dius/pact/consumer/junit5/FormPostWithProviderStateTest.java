package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.FormPostBuilder;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "FormPostProvider", pactVersion = PactSpecVersion.V3)
public class FormPostWithProviderStateTest {
  @Pact(consumer = "FormPostConsumer")
  public RequestResponsePact formpost(PactDslWithProvider builder) {
    return builder
      .given("provider state 1")
      .uponReceiving("FORM POST request with provider state")
        .path("/form")
        .method("POST")
        .body(
          new FormPostBuilder()
            .parameterFromProviderState("value", "value", "1000"))
      .willRespondWith()
        .status(200)
      .toPact();
  }

  @Test
  void testFormPost(MockServer mockServer) throws IOException {
    HttpResponse httpResponse = Request.post(mockServer.getUrl() + "/form")
      .bodyForm(
        new BasicNameValuePair("value", "1000")).execute().returnResponse();
    assertThat(httpResponse.getCode(), is(equalTo(200)));
  }
}
