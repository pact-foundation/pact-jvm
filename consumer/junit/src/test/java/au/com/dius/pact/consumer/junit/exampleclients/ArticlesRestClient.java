package au.com.dius.pact.consumer.junit.exampleclients;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpResponse;

import java.io.IOException;

public class ArticlesRestClient {

    public HttpResponse getArticles(String baseUrl)
      throws IOException {

        CloseableHttpClient httpClient = HttpClient.insecureHttpClient();
        return Request.get(baseUrl + "/articles.json")
                .execute(httpClient).returnResponse();
    }
}
