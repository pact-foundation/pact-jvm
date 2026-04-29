// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block24
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;

@Disabled("Doctest stub — see README.md block block24")
class README_java_block24_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:block24
        newJsonBody((o) -> {
            o.stringValue("foo", "Foo");
            o.stringValue("bar", "Bar");
        }).build();
        // @DOCTEST-END
    }
}
