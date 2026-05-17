// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 12
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.junit5.doctest;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Disabled;

@Disabled("Doctest stub — see README.md block 12")
class README_java_block12_Test {

//    @Test
//    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:block12
        @Pact(consumer = "ArticlesClient")
        V4Pact createPact(PactBuilder builder) {
          return builder
            .expectsToReceiveHttpInteraction("create article", http -> http
                .withRequest(req -> req.method("POST").path("/articles"))
                .willRespondWith(res -> res.status(201))
                .reference("openapi", "operationId", "createArticle")
                .reference("openapi", "tag", "articles")
                .reference("jira", "ticket", "PROJ-123")
            )
            .toPact();
        }
        // @DOCTEST-END
//    }
}
