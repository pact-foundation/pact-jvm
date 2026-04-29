// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block04
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Doctest stub — see README.md block block04")
class README_java_block04_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:block04
            DslPart body = new PactDslJsonBody()
                .minArrayLike("users", 2)
                    .id()
                    .stringType("name")
                .closeObject()
                .closeArray();
        // @DOCTEST-END
    }
}
