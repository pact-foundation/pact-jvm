// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block13
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.junit5.doctest;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Disabled;

@Disabled("Doctest stub — see README.md block block13")
class README_java_block13_Test {

//    @Test
//    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:block13
        @Pact(consumer = "ArticlesClient")
        V4Pact createPact(PactBuilder builder) {
          return builder
            .expectsToReceiveMessageInteraction("article created event", message -> message
              .withContents( contents -> contents
                .withContent(new PactDslJsonBody().stringType("title")) )
              .reference("asyncapi", "messageId", "ArticleCreated")
            )
            .toPact();
        }
        // @DOCTEST-END
//    }
}
