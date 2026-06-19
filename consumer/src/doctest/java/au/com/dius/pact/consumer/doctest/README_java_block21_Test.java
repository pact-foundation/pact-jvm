// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block21
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonArray;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block block21")
class README_java_block21_Test {

    @Test
    void block() throws Exception {
      PactDslWithProvider builder = ConsumerPactBuilder
        .consumer("Some Consumer")
        .hasPactWith("Some Provider");
        // @DOCTEST-BEGIN README.md:java:block21
        // import au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonArray
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
