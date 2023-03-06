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

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonArrayMinLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(pactVersion = PactSpecVersion.V3)
class MixedPactAndNonPactTest {
  @Pact(consumer = "ApiConsumer")
  public RequestResponsePact defaultValues(PactDslWithProvider builder) {
    return builder
      .uponReceiving("GET request to retrieve default values")
      .path("/api/test")
      .willRespondWith()
      .status(200)
      .body(newJsonArrayMinLike(1, values -> values.object(value -> {
          value.numberType("id", 32432);
          value.stringType("name", "testId254");
          value.numberType("size", 1445211);
        }
      )).build())
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "defaultValues")
  void testDefaultValues(MockServer mockServer) throws IOException {
    HttpResponse response = Request.get(mockServer.getUrl() + "/api/test").execute().returnResponse();
    assertEquals(response.getCode(), 200);
  }

  @Test
  @PactIgnore
  void nonPactTest() {
    assertThat(true, is(not(false))); // otherwise, the universe will end
  }
}
