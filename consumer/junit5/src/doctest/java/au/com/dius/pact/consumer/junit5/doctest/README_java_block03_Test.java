// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 3
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.junit5.doctest;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 3")
class README_java_block03_Test {

//    @Test
//    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:3
            @Pact(provider="ArticlesProvider", consumer="test_consumer")
            public RequestResponsePact createPact(PactDslWithProvider builder) {
                return builder
                    .given("test state")
                    .uponReceiving("ExampleJavaConsumerPactTest test interaction")
                        .path("/articles.json")
                        .method("GET")
                    .willRespondWith()
                        .status(200)
                        .body("{\"responsetest\": true}")
                    .toPact();
            }
        // @DOCTEST-END
//    }
}
