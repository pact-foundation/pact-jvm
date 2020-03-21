package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.RequestResponsePact;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ArticlesProvider", port = "1234")
public class ArticlesTest {
  private Map<String, String> headers = MapUtils.putAll(new HashMap<>(), new String[] {
    "Content-Type", "application/json"
  });

  @BeforeEach
  public void setUp(MockServer mockServer) {
    assertThat(mockServer, is(notNullValue()));
  }

  @Pact(consumer = "ArticlesConsumer")
  public RequestResponsePact articles(PactDslWithProvider builder) {
    return builder
      .given("Articles exist")
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

  @Pact(consumer = "ArticlesConsumer")
  public RequestResponsePact articlesDoNotExist(PactDslWithProvider builder) {
    return builder
      .given("No articles exist")
      .uponReceiving("retrieving article data")
      .path("/articles.json")
      .method("GET")
      .willRespondWith()
      .headers(headers)
      .status(404)
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "articles")
  void testArticles(MockServer mockServer) throws IOException {
    HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/articles.json").execute().returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(equalTo(200)));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()),
      is(equalTo("{\"articles\":[{\"variants\":{\"0032\":{\"description\":\"sample description\"}}}]}")));
  }

  @Test
  @PactTestFor(pactMethod = "articlesDoNotExist")
  void testArticlesDoNotExist(MockServer mockServer) throws IOException {
    HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/articles.json").execute().returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(equalTo(404)));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(equalTo("")));
  }
}
