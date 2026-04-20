// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 5
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static au.com.dius.pact.consumer.dsl.DslPart.regex;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 5")
class README_java_block05_Test {

    @Test
    void block() throws Exception {
        new PactDslJsonBody()
        // @DOCTEST-BEGIN README.md:java:5
        .arrayContaining("actions")
          .object()
            .stringValue("name", "update")
            .stringValue("method", "PUT")
            .matchUrl("href", "http://localhost:9000", "orders", regex("\\d+", "1234"))
          .closeObject()
          .object()
            .stringValue("name", "delete")
            .stringValue("method", "DELETE")
            .matchUrl("href", "http://localhost:9000", "orders", regex("\\d+", "1234"))
          .closeObject()
        .closeArray()
        // @DOCTEST-END
        ;
    }
}
