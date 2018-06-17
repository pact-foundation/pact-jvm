package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactFragment;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class Defect320Test extends ConsumerPactTest {

  public PactFragment createFragment(PactDslWithProvider builder) {
    DslPart requestDSL = new PactDslJsonBody()
      .stringType("id")
      .stringType("method")
      .stringType("jsonrpc", "2.0")
      .array("params")
        .stringType("QIZ");
    return builder
        .given("test state")
        .uponReceiving("A request for json")
            .path("/json")
            .method("PUT")
            .body(requestDSL)
        .willRespondWith()
            .status(200)
        .toFragment();
  }

  @Override
  protected String providerName() {
    return "320_provider";
  }

  @Override
  protected String consumerName() {
    return "test_consumer";
  }

  @Override
  protected void runTest(String url) throws IOException {
    assertEquals(200, Request.Put(url + "/json")
      .addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType())
      .bodyString("{" +
          "\"id\": \"any string\"," +
          "\"method\": \"any string\"," +
          "\"jsonrpc\": \"2.0\"," +
          "\"params\": [\"any string\"]}",
        ContentType.APPLICATION_JSON)
      .execute().returnResponse().getStatusLine().getStatusCode());
  }
}
