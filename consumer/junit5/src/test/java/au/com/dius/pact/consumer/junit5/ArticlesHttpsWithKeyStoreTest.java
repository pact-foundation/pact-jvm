package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ArticlesProvider", https = true,
        keyStorePath = "src/test/resources/keystore/pact-jvm-2048.jks",
        keyStoreAlias = "localhost",
        keyStorePassword = "coderswerehere",
        privateKeyPassword = "coderswerehere")
public class ArticlesHttpsWithKeyStoreTest {
  private Map<String, String> headers = MapUtils.putAll(new HashMap<>(), new String[] {
    "Content-Type", "application/json"
  });

  @BeforeEach
  public void setUp(MockServer mockServer) {
    assertThat(mockServer, is(notNullValue()));
    assertThat(mockServer.getUrl(), startsWith("https://"));
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
  void testArticles(MockServer mockServer) throws IOException, GeneralSecurityException {
    HttpResponse httpResponse = get(mockServer.getUrl() + "/articles.json");
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(equalTo(200)));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()),
      is(equalTo("{\"articles\":[{\"variants\":{\"0032\":{\"description\":\"sample description\"}}}]}")));
  }

  private HttpResponse get(String url) throws IOException, GeneralSecurityException {
    return httpClient().execute(new HttpGet(url));
  }

  private HttpClient httpClient() throws GeneralSecurityException {
    SSLSocketFactory socketFactory = new SSLSocketFactory(new TrustSelfSignedStrategy());
    return HttpClientBuilder.create()
      .setSSLSocketFactory(socketFactory)
      .build();
  }

  @Test
  @PactTestFor(pactMethod = "articlesDoNotExist")
  void testArticlesDoNotExist(MockServer mockServer) throws IOException, GeneralSecurityException {
    HttpResponse httpResponse = get(mockServer.getUrl() + "/articles.json");
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(equalTo(404)));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(equalTo("")));
  }
}
