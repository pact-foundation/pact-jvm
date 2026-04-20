// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 2
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 2")
class README_java_block02_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:2
        .consumer("Some Consumer")
        .hasPactWith("Some Provider")
        .given("a certain state on the provider")
            .uponReceiving("a request for something")
                .path("/hello")
                .method("POST")
                .body("{\"name\": \"harry\"}")
            .willRespondWith()
                .status(200)
                .body("{\"hello\": \"harry\"}")
            .uponReceiving("another request for something")
                .path("/hello")
                .method("POST")
                .body("{\"name\": \"harry\"}")
            .willRespondWith()
                .status(200)
                .body("{\"hello\": \"harry\"}")
            .
            .
            .
        .toPact()
        // @DOCTEST-END
    }
}
