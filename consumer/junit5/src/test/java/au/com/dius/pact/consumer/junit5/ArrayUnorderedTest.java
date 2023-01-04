package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonArrayUnordered;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(PactConsumerTestExt.class)
public class ArrayUnorderedTest {
  @Test
  @PactTestFor(
    providerName = "PactProvider",
    pactMethod = "pactPassIdArray",
    pactVersion = PactSpecVersion.V3)
  void testContract(MockServer mockServer) throws IOException {
    HttpResponse response = Request.post(mockServer.getUrl() + "/passIdArray")
      .bodyString("[{\"id\":\"123\"}]", ContentType.APPLICATION_JSON).execute().returnResponse();
    assertEquals(response.getCode(), 200);
  }

  @Pact(consumer = "PactConsumer")
  RequestResponsePact pactPassIdArray(PactDslWithProvider provider) {
    return provider
      .uponReceiving("publish entity")
      .path("/passIdArray")
      .method("POST")
      .body(
        newJsonArrayUnordered(refs ->
          refs.object(ref -> ref.stringType("id", "123"))
        ).build()
      )
      .willRespondWith()
      .status(200)
      .toPact();
  }
}
