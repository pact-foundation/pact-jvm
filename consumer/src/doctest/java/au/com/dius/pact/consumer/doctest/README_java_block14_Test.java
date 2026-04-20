// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 14
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 14")
class README_java_block14_Test {

    @Test
    void block() throws Exception {
      new PactDslJsonBody()
        // @DOCTEST-BEGIN README.md:java:14
            .valueFromProviderState("userId", "userId", 100) // will look value using userId as the key
        // @DOCTEST-END
      ;
    }
}
