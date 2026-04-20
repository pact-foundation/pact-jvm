// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 10
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.junit5.doctest;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 10")
class README_java_block10_Test {

    @Test
    void block() throws Exception {
        MessagePactBuilder builder = new MessagePactBuilder()
          .consumer("consumer")
          .hasPactWith("provider");
        // @DOCTEST-BEGIN README.md:java:10
        builder.given("Some Provider State")
            .expectsToReceive("a test message")
            .withContent(new PactDslJsonBody()
              .stringValue("testParam1", "value1")
              .stringValue("testParam2", "value2"))
            .toPact();
        // @DOCTEST-END
    }
}
