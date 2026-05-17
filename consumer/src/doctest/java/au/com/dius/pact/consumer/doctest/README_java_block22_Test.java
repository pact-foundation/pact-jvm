// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block22
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;

@Disabled("Doctest stub — see README.md block block22")
class README_java_block22_Test {

    @Test
    void block() throws Exception {
      PactDslWithProvider builder = ConsumerPactBuilder
        .consumer("Some Consumer")
        .hasPactWith("Some Provider");
        // @DOCTEST-BEGIN README.md:java:block22
        // import au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody
        builder.given("some state")
                .uponReceiving("a request")
                .path("/my-app/my-service")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(newJsonBody((o) -> {
                    o.stringValue("foo", "Foo");
                    o.stringValue("bar", "Bar");
                }).build());
        // @DOCTEST-END
    }
}
