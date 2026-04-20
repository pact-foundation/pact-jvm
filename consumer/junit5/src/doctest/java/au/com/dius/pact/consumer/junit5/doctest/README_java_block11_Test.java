// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 11
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.junit5.doctest;

import au.com.dius.pact.consumer.MessagePactBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 11")
class README_java_block11_Test {

    @Test
    void block() throws Exception {
      MessagePactBuilder builder = new MessagePactBuilder()
        .consumer("consumer")
        .hasPactWith("provider");
        // @DOCTEST-BEGIN README.md:java:11
        builder.given("SomeProviderState")
            .expectsToReceive("a test message with metadata")
            .withMetadata(md -> {
                md.add("metadata1", "metadataValue1");
                md.add("metadata2", "metadataValue2");
                md.add("metadata3", 10L);
                md.matchRegex("partitionKey", "[A-Z]{3}\\d{2}", "ABC01");
            })
            .withContent("{\"value\": \"test\"}")
            .toPact();
        // @DOCTEST-END
    }
}
