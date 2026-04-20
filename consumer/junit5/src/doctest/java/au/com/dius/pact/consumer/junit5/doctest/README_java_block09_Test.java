// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 9
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.junit5.doctest;

import au.com.dius.pact.consumer.MessagePactBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 9")
class README_java_block09_Test {

    @Test
    void block() throws Exception {
        MessagePactBuilder builder = new MessagePactBuilder()
          .consumer("consumer")
          .hasPactWith("provider");
        // @DOCTEST-BEGIN README.md:java:9
        builder.given("Some Provider State")
            .expectsToReceive("a test message")
            .withContent("{\"value\": \"test\"}")
            .toPact();
        // @DOCTEST-END
    }
}
