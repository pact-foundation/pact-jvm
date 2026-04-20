// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 8
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.junit5.doctest;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 8")
class README_java_block08_Test {

    @Test
    void block() throws Exception {
      new PactDslJsonBody()
        // @DOCTEST-BEGIN README.md:java:8
            .valueFromProviderState("userId", "userId", 100) // will lookup value using userId as the key
        // @DOCTEST-END
      ;
    }
}
