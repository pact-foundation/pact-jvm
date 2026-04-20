// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 22
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 22")
class README_java_block22_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:22
        new PactDslJsonArray()
            .array()
            .stringValue("a1")
            .stringValue("a2")
            .closeArray()
            .array()
            .numberValue(1)
            .numberValue(2)
            .closeArray()
            .array()
            .object()
            .stringValue("foo", "Foo")
            .closeObject()
            .closeArray();
        // @DOCTEST-END
    }
}
