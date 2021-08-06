package au.com.dius.pact.consumer.junit.exampleclients;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class ProviderClient {

    private final String url;

    public ProviderClient(String url) {
        this.url = url;
    }

    public Map hello(String body) throws IOException {
        String response = Request.post(url + "/hello")
                .bodyString(body, ContentType.APPLICATION_JSON)
                .execute().returnContent().asString(Charset.defaultCharset());
        return new ObjectMapper().readValue(response, HashMap.class);
    }
}
