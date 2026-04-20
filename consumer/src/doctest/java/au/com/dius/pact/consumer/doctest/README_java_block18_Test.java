// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 18
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 18")
class README_java_block18_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:18
        
        import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonArray;
        
        ...
        
        PactDslWithProvider builder = ...
        builder.given("some state")
                .uponReceiving("a request")
                .path("/my-app/my-service")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(newJsonArray((a) -> {
                    a.stringValue("a1");
                    a.stringValue("a2");
                }).build());
        // @DOCTEST-END
    }
}
