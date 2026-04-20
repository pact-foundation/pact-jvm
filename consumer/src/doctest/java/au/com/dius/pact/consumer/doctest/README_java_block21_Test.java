// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 21
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 21")
class README_java_block21_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:21
        newJsonBody((o) -> {
            o.stringValue("foo", "Foo");
            o.stringValue("bar", "Bar");
        }).build();
        // @DOCTEST-END
    }
}
