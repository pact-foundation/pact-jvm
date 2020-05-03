package au.com.dius.pact.consumer.junit.examples;

import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.exampleclients.ArticlesRestClient;
import au.com.dius.pact.consumer.junit.PactHttpsProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import org.apache.commons.collections4.MapUtils;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Example taken from https://groups.google.com/forum/#!topic/pact-support/-Kk_OxvcJQY
 */
public class ArticlesHttpsTest {
    Map<String, String> headers = MapUtils.putAll(new HashMap<>(),
        new String[]{"Content-Type", "application/json"});

    @Rule
    public PactHttpsProviderRule provider = new PactHttpsProviderRule("ArticlesProvider", "localhost", 1234, true, PactSpecVersion.V3, this);

    @Pact(provider = "ArticlesProvider", consumer = "ArticlesConsumer")
    public RequestResponsePact articlesFragment(PactDslWithProvider builder) {
        return builder
            .given("Pact for Issue 313")
            .uponReceiving("retrieving article data")
            .path("/articles.json")
            .method("GET")
            .willRespondWith()
            .headers(headers)
            .status(200)
            .body(
                new PactDslJsonBody()
                    .minArrayLike("articles", 1)
                        .object("variants")
                            .eachKeyLike("0032")
                                .stringType("description", "sample description")
                            .closeObject()
                        .closeObject()
                    .closeObject()
                    .closeArray()
            )
            .toPact();
    }

    @PactVerification("ArticlesProvider")
    @Test
    public void testArticles() throws IOException {
        ArticlesRestClient providerRestClient = new ArticlesRestClient();
        providerRestClient.getArticles("https://localhost:1234");
    }
}
