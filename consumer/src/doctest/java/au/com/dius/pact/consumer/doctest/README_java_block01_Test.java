// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 1
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.*;
import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.RequestResponsePact;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;
import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Disabled;
// TODO: add required imports

class ConsumerClient {
  private String url;

  public ConsumerClient(String url) {
    this.url = url;
  }

  public Map post(String path, String body, ContentType mimeType) throws IOException {
    return Map.of();
  }
}

@Disabled("Doctest stub — see README.md block 1")
public class README_java_block01_Test {

//    @Test
//    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:1
        class PactTest {
        
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
            PactVerificationResult result = runConsumerTest(pact, config, new PactTestRun() {
              @Override
              public Object run(@NotNull MockServer mockServer, PactTestExecutionContext context) throws IOException {
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
        
            assert(result instanceof PactVerificationResult.Ok);
          }
        
        }
        // @DOCTEST-END
//    }
}
