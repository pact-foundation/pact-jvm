package au.com.dius.pact.consumer.exampleclients;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;

import java.io.IOException;

public class ArticlesRestClient {

    public HttpResponse getArticles(String baseUrl)
        throws IOException {

        return Request.Get(baseUrl + "/articles.json")
                .execute().returnResponse();
    }
}
