// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block17
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Doctest stub — see README.md block block17")
class README_java_block17_Test {

    @Test
    void block() throws Exception {
      new PactDslJsonBody()
        // @DOCTEST-BEGIN README.md:java:block17
            .valueFromProviderState("userId", "userId", 100) // will look value using userId as the key
        // @DOCTEST-END
      ;
    }
}
