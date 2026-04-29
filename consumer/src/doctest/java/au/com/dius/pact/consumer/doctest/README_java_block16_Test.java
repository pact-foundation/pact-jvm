// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block16
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Doctest stub — see README.md block block16")
class README_java_block16_Test {

    @Test
    void block() throws Exception {
      ConsumerPactBuilder
        .consumer("Some Consumer")
        .hasPactWith("Some Provider")
        .uponReceiving("a request")
        // @DOCTEST-BEGIN README.md:java:block16
            .pathFromProviderState("/api/users/${id}", "/api/users/100")
        // @DOCTEST-END
      ;
    }
}
