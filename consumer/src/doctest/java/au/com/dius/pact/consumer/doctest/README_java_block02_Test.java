// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block02
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Doctest stub — see README.md block block02")
class README_java_block02_Test {

    @Test
    void block() throws Exception {
        ConsumerPactBuilder
        // @DOCTEST-BEGIN README.md:java:block02
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
        //    .
        //    .
        //    .
        .toPact()
        // @DOCTEST-END
        ;
    }
}
