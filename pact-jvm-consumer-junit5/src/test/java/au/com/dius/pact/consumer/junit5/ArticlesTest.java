package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ArticlesProvider", port = "1234")
public class ArticlesTest {
  private Map<String, String> headers = MapUtils.putAll(new HashMap<>(), new String[] {
    "Content-Type", "application/json"
  });

  @Pact(provider = "ArticlesProvider", consumer = "ArticlesConsumer")
  public RequestResponsePact articles(PactDslWithProvider builder) {
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

  @Test
  void testArticles(@PactMockServer MockServer mockServer) throws IOException {
    HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/articles.json").execute().returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(equalTo(200)));
  }
}
