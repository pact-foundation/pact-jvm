package au.com.dius.pact.consumer;


import org.apache.http.client.fluent.Request;

import java.io.IOException;

public class ConsumerClient{
    private String url;

    public ConsumerClient(String url) {
        this.url = url;
    }

    public String get(String path) throws IOException {
        return Request.Get(url + path).execute().returnContent().asString();
    }
}
