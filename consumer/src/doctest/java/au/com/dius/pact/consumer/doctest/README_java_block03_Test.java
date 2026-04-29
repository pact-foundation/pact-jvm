// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block03
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Doctest stub — see README.md block block03")
class README_java_block03_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:block03
        PactDslJsonBody body = new PactDslJsonBody()
            .stringType("name")
            .booleanType("happy")
            .hexValue("hexCode")
            .id()
            .ipAddress("localAddress")
            .numberValue("age", 100)
            .datetime("timestamp");
        // @DOCTEST-END
    }
}
