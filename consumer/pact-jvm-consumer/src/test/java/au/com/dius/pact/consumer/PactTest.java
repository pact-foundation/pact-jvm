package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.RequestResponsePact;
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class PactTest {

  @Test
  public void testPact() {
    RequestResponsePact pact = ConsumerPactBuilder
      .consumer("Some Consumer")
      .hasPactWith("Some Provider")
      .uponReceiving("a request to say Hello")
      .path("/hello")
      .method("POST")
      .body("{\"name\": \"harry\"}")
      .willRespondWith()
      .status(200)
      .body("{\"hello\": \"harry\"}")
      .toPact();

    MockProviderConfig config = MockProviderConfig.createDefault();
    PactVerificationResult result = runConsumerTest(pact, config, new PactTestRun<Boolean>() {
      @Override
      public Boolean run(@NotNull MockServer mockServer, PactTestExecutionContext context) throws IOException {
        Map expectedResponse = new HashMap();
        expectedResponse.put("hello", "harry");
        assertEquals(expectedResponse, new ConsumerClient(mockServer.getUrl()).post("/hello",
            "{\"name\": \"harry\"}", ContentType.APPLICATION_JSON));
        return true;
      }
    });

    if (result instanceof PactVerificationResult.Error) {
      throw new RuntimeException(((PactVerificationResult.Error)result).getError());
    }

    assertThat(result, is(instanceOf(PactVerificationResult.Ok.class)));
  }

}
