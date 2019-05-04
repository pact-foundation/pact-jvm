package au.com.dius.pact.consumer.exampleclients;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.InsecureHttpsRequest;

import java.io.IOException;

public class ArticlesRestClient {

    public HttpResponse getArticles(String baseUrl)
        throws IOException {

        return InsecureHttpsRequest.httpsGet(baseUrl + "/articles.json")
                .execute().returnResponse();
    }
}
