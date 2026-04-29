// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block12
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Doctest stub — see README.md block block12")
class README_java_block12_Test {

    @Test
    void block() throws Exception {
      ConsumerPactBuilder
        .consumer("Some Consumer")
        .hasPactWith("Some Provider")
        // @DOCTEST-BEGIN README.md:java:block12
          .given("test state")
            .uponReceiving("a test interaction")
                .path("/hello")
                .method("POST")
                .matchQuery("a", "\\d+", "100")
                .matchQuery("b", "[A-Z]", "X")
                .body("{\"name\": \"harry\"}")
            .willRespondWith()
                .status(200)
                .body("{\"hello\": \"harry\"}")
        // @DOCTEST-END
        ;
    }
}
