package au.com.dius.pact.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConsumerClient{
    private String url;

    public ConsumerClient(String url) {
        this.url = url;
    }

    public Map get(String path) throws IOException {
        return jsonToMap(Request.Get(url + path)
                .addHeader("testreqheader", "testreqheadervalue")
                .execute().returnContent().asString());
    }

    public Map post(String path, String body) throws IOException {
        String respBody = Request.Post(url + path)
                .addHeader("testreqheader", "testreqheadervalue")
                .bodyString(body, ContentType.APPLICATION_JSON)
                .execute().returnContent().asString();
        return jsonToMap(respBody);
    }

    private HashMap jsonToMap(String respBody) throws IOException {
        return new ObjectMapper().readValue(respBody, HashMap.class);
    }

    public int options(String path) throws IOException {
        return Request.Options(url + path)
                .addHeader("testreqheader", "testreqheadervalue")
                .execute().returnResponse().getStatusLine().getStatusCode();
    }
}
