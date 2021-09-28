package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "NullValuesProvider", pactVersion = PactSpecVersion.V3)
public class NullValuesTest {
  @Pact(consumer = "NullValuesConsumer")
  public RequestResponsePact pactWithNullValues(PactDslWithProvider builder) {
    return builder
      .uponReceiving("retrieving transaction request")
        .path("/")
        .method("GET")
      .willRespondWith()
        .status(200)
        .body(
          new PactDslJsonBody()
              .object("transaction")
                .nullValue("description")
                .object("amount")
                  .nullValue("amount")
                  .nullValue("salesAmount")
                  .nullValue("surchargeAmount")
                  .nullValue("currency")
                .closeObject()
              .closeObject()
        )
      .toPact();
  }

  @Test
  void testArticles(MockServer mockServer) throws IOException {
    ClassicHttpResponse httpResponse = (ClassicHttpResponse) Request.get(mockServer.getUrl()).execute().returnResponse();
    assertThat(httpResponse.getCode(), is(equalTo(200)));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()),
      is(equalTo("{\"transaction\":{\"amount\":{\"amount\":null,\"currency\":null,\"salesAmount\":null,\"surchargeAmount\":null},\"description\":null}}")));
  }
}
