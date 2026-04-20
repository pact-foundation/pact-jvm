// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 6
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 6")
class README_java_block06_Test {

    @Test
    void block() throws Exception {
        ConsumerPactBuilder
        // @DOCTEST-BEGIN README.md:java:6
        .consumer("Some Consumer")
        .hasPactWith("Some Provider")
            .uponReceiving("a request for a basic JSON value")
                .path("/hello")
            .willRespondWith()
                .status(200)
                .body(PactDslJsonRootValue.integerType())
        // @DOCTEST-END
        ;
    }
}
