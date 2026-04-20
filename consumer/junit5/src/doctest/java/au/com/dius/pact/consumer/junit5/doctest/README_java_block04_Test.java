// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 4
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.junit5.doctest;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 4")
class README_java_block04_Test {

//    @Test
//    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:4
            @Pact(provider="ArticlesProvider", consumer="test_consumer")
            public V4Pact createPact(PactDslWithProvider builder) {
                return builder
                    .given("test state")
                    .uponReceiving("ExampleJavaConsumerPactTest test interaction")
                        .path("/articles.json")
                        .method("GET")
                    .willRespondWith()
                        .status(200)
                        .body("{\"responsetest\": true}")
                    .toPact(V4Pact.class);
            }
        // @DOCTEST-END
//    }
}
