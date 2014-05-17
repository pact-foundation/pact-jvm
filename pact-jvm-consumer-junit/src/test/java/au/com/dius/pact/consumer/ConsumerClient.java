package au.com.dius.pact.consumer;

import org.apache.http.client.fluent.Request;

import java.io.IOException;

public class ConsumerClient{
    private String url;

    public ConsumerClient(String url) {
        this.url = url;
    }

    public String get(String path) throws IOException {
        return Request.Get(url + path)
                .addHeader("testreqheader", "testreqheadervalue")
                .execute().returnContent().asString();
    }

    public int options(String path) throws IOException {
        return Request.Options(url + path)
                .addHeader("testreqheader", "testreqheadervalue")
                .execute().returnResponse().getStatusLine().getStatusCode();
    }
}
