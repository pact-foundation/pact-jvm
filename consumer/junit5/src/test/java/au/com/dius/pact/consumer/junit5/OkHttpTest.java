package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "example-provider", pactVersion = PactSpecVersion.V4)
public class OkHttpTest {
  private final OkHttpClient client = new OkHttpClient();

  @Pact(consumer = "example-consumer")
  public V4Pact createOrderPact(PactDslWithProvider builder) {
    return builder
      .given("provider accepts an order")
      .uponReceiving("a request to create an order")
      .path("/orders")
      .method("POST")
      .headers(Map.of("Content-Type", "application/json"))
      .body(new PactDslJsonBody()
        .stringType("customerId", "cust-123")
        .decimalType("amount", 100.00))
      .willRespondWith()
      .status(200)
      .headers(Map.of("Content-Type", "application/json"))
      .body(new PactDslJsonBody()
        .stringType("orderId", "order-456")
        .stringValue("status", "CREATED"))
      .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(pactMethod = "createOrderPact")
  void should_create_order(MockServer mockServer) throws IOException {
    Request request = new Request.Builder()
      .url(mockServer.getUrl() + "/orders")
      .post(RequestBody.create(
        "{\"customerId\":\"cust-123\",\"amount\":100.00}",
        MediaType.get("application/json")))
      .build();

    try (Response response = client.newCall(request).execute()) {
      assertThat(response.code(), is(equalTo(200)));
    }
  }
}
