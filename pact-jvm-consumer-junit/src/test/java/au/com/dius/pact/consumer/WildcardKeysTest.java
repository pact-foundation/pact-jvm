package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import com.google.common.collect.Sets;
import groovy.json.JsonSlurper;
import org.apache.http.client.fluent.Request;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

public class WildcardKeysTest {

    private static final String APPLICATION_JSON = "application/json";

    @Rule
    public PactProviderRuleMk2 provider = new PactProviderRuleMk2("WildcardKeysProvider", "localhost", 8081, this);

    @Pact(provider="WildcardKeysProvider", consumer="WildcardKeysConsumer")
    public RequestResponsePact createFragment(PactDslWithProvider builder) {
      DslPart body = new PactDslJsonBody()
        .eachLike("articles")
          .eachLike("variants")
            .eachKeyMappedToAnArrayLike("001")
              .eachLike("bundles")
                .eachKeyLike("001-A")
                  .stringType("description", "Some Description")
                    .eachLike("referencedArticles")
                      .id("bundleId", 23456L)
                      .eachKeyLike("001-A-1", PactDslJsonRootValue.id(12345L))
                      .closeObject()
                    .closeArray()
                  .closeObject()
                .closeObject()
              .closeArray()
              .closeObject()
            .closeArray()
            .closeObject()
          .closeArray()
          .closeObject()
        .closeArray()
        .object("foo")
          .eachKeyLike("001", PactDslJsonRootValue.numberType(42))
        .closeObject();

      RequestResponsePact pact = builder
        .uponReceiving("a request for an article")
        .path("/")
        .method("GET")
        .willRespondWith()
        .status(200)
        .body(body)
        .toPact();

      MatcherTestUtils.assertResponseMatcherKeysEqualTo(pact, "body",
        "$.articles",
        "$.articles[*].variants",
        "$.articles[*].variants[*].*",
        "$.articles[*].variants[*].*[*].bundles",
        "$.articles[*].variants[*].*[*].bundles[*].*",
        "$.articles[*].variants[*].*[*].bundles[*].*.description",
        "$.articles[*].variants[*].*[*].bundles[*].*.referencedArticles",
        "$.articles[*].variants[*].*[*].bundles[*].*.referencedArticles[*].*",
        "$.articles[*].variants[*].*[*].bundles[*].*.referencedArticles[*].bundleId",
        "$.foo.*"
      );

      return pact;
    }

    @Test
    @PactVerification("WildcardKeysProvider")
    public void runTest() throws IOException {
      String result = Request.Get("http://localhost:8081/")
        .addHeader("Accept", APPLICATION_JSON)
        .execute().returnContent().asString();
      Map<String, Object> body = (Map<String, Object>) new JsonSlurper().parseText(result);

      assertThat(body, hasKey("foo"));
      Map<String, Object> foo = (Map<String, Object>) body.get("foo");
      assertThat(foo, hasKey("001"));
      assertThat(foo.get("001"), Matchers.<Object>is(42));
      assertThat(body, hasKey("articles"));
      List articles = (List) body.get("articles");
      assertThat(articles.size(), is(1));
      Map<String, Object> article = (Map<String, Object>) articles.get(0);
      assertThat(article, hasKey("variants"));
      List variants = (List) article.get("variants");
      assertThat(variants.size(), is(1));
      Map<String, Object> variant = (Map<String, Object>) variants.get(0);
      assertThat(variant.keySet(), is(equalTo((Set<String>) Sets.newHashSet("001"))));
      List variant001 = (List) variant.get("001");
      assertThat(variant001.size(), is(1));
      Map<String, Object> firstVariant001 = (Map<String, Object>) variant001.get(0);
      assertThat(firstVariant001, hasKey("bundles"));
      List bundles = (List) firstVariant001.get("bundles");
      assertThat(bundles.size(), is(1));
      Map<String, Object> bundle = (Map<String, Object>) bundles.get(0);
      assertThat(bundle.keySet(), is(equalTo((Set<String>) Sets.newHashSet("001-A"))));
      Map<String, Object> bundle001A = (Map<String, Object>) bundle.get("001-A");
      assertThat(bundle001A.get("description").toString(), is("Some Description"));
      assertThat(bundle001A, hasKey("referencedArticles"));
      List referencedArticles = (List) bundle001A.get("referencedArticles");
      assertThat(referencedArticles.size(), is(1));
      Map<String, Object> referencedArticle = (Map<String, Object>) referencedArticles.get(0);
      assertThat(referencedArticle, hasKey("bundleId"));
      assertThat(referencedArticle.get("bundleId").toString(), is("23456"));
    }
}
