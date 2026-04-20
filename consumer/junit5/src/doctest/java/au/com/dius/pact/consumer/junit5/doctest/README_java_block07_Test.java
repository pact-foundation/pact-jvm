// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 7
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.junit5.doctest;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 7")
class README_java_block07_Test {

    @Test
    void block() throws Exception {
      ConsumerPactBuilder
        .consumer("Some Consumer")
        .hasPactWith("Some Provider")
        .uponReceiving("Some Request")
        // @DOCTEST-BEGIN README.md:java:7
            .pathFromProviderState("/api/users/${id}", "/api/users/100")
        // @DOCTEST-END
      ;
    }
}
