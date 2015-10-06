package au.com.dius.pact.consumer.exampleclients;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProviderClient {

    private final String url;

    public ProviderClient(String url) {
        this.url = url;
    }

    public Map hello(String body) throws IOException {
        String response = Request.Post(url + "/hello")
                .bodyString(body, ContentType.APPLICATION_JSON)
                .execute().returnContent().asString();
        return new ObjectMapper().readValue(response, HashMap.class);
    }
}
