package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.model.RequestResponsePact;
import org.apache.http.client.HttpResponseException;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.fail;

public class PactConsumer400Test {

    @Rule
    public PactProviderRuleMk2 rule = new PactProviderRuleMk2("test_provider", this);

    @Pact(provider="test_provider", consumer="test_consumer")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        return builder
          .uponReceiving("a request for a non-existent path")
          .path("/does-not-exist")
          .method("GET")
          .willRespondWith()
          .status(400)
          .toPact();
    }

    @Test(expected = HttpResponseException.class)
    @PactVerification("test_provider")
    public void runTestAndLetJUnitHandleTheException() throws IOException {
        new ConsumerClient("http://localhost:" + rule.getPort()).getAsMap("/does-not-exist", "");
    }

    @Test
    @PactVerification("test_provider")
    public void runTestAndHandleTheException() throws IOException {
      try {
        new ConsumerClient("http://localhost:" + rule.getPort()).getAsMap("/does-not-exist", "");
        fail("Should have thrown an exception");
      } catch (HttpResponseException e) {
        // correct behaviour
      }
    }
}
