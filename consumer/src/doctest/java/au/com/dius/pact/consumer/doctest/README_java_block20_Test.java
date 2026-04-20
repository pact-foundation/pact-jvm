// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 20
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 20")
class README_java_block20_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:20
        new PactDslJsonBody()
            .stringValue("foo", "Foo")
            .stringValue("bar", "Bar")
        // @DOCTEST-END
        ;
    }
}
