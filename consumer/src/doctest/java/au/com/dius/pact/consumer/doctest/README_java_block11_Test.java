// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block11
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Doctest stub — see README.md block block11")
class README_java_block11_Test {

    @Test
    void block() throws Exception {
        ConsumerPactBuilder
          .consumer("Some Consumer")
          .hasPactWith("Some Provider")
        // @DOCTEST-BEGIN README.md:java:block11
          .given("test state")
            .uponReceiving("a test interaction")
                .path("/hello")
                .method("POST")
                .matchHeader("testreqheader", "test.*value")
                .body("{\"name\": \"harry\"}")
            .willRespondWith()
                .status(200)
                .body("{\"hello\": \"harry\"}")
                .matchHeader("Location", ".*/hello/[0-9]+", "/hello/1234")
        // @DOCTEST-END
      ;
    }
}
