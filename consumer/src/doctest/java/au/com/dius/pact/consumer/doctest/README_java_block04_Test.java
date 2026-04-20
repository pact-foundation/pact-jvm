// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 4
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 4")
class README_java_block04_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:4
            DslPart body = new PactDslJsonBody()
                .minArrayLike("users")
                    .id()
                    .stringType("name")
                .closeObject()
                .closeArray();
        // @DOCTEST-END
    }
}
