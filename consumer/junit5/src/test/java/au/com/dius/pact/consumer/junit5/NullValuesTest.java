package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "NullValuesProvider")
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
    HttpResponse httpResponse = Request.Get(mockServer.getUrl()).execute().returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(equalTo(200)));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent(), Charset.defaultCharset()),
      is(equalTo("{\"transaction\":{\"amount\":{\"amount\":null,\"currency\":null,\"salesAmount\":null,\"surchargeAmount\":null},\"description\":null}}")));
  }
}
