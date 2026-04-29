// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block23
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Doctest stub — see README.md block block23")
class README_java_block23_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:block23
        new PactDslJsonBody()
            .stringValue("foo", "Foo")
            .stringValue("bar", "Bar")
        // @DOCTEST-END
        ;
    }
}
