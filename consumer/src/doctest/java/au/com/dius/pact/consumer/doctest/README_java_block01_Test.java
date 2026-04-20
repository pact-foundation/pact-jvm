// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 1
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 1")
class README_java_block01_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:1
        import au.com.dius.pact.model.MockProviderConfig;
        import au.com.dius.pact.model.RequestResponsePact;
        import org.apache.http.entity.ContentType;
        import org.jetbrains.annotations.NotNull;
        import org.junit.Test;
        
        import java.io.IOException;
        import java.util.HashMap;
        import java.util.Map;
        
        import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;
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
            PactVerificationResult result = runConsumerTest(pact, config, new PactTestRun() {
              @Override
              public void run(@NotNull MockServer mockServer) throws IOException {
                Map expectedResponse = new HashMap();
                expectedResponse.put("hello", "harry");
                assertEquals(expectedResponse, new ConsumerClient(mockServer.getUrl()).post("/hello",
                    "{\"name\": \"harry\"}", ContentType.APPLICATION_JSON));
              }
            });
        
            if (result instanceof PactVerificationResult.Error) {
              throw new RuntimeException(((PactVerificationResult.Error)result).getError());
            }
        
            assertEquals(PactVerificationResult.Ok.INSTANCE, result);
          }
        
        }
        // @DOCTEST-END
    }
}
