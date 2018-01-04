package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import groovy.json.JsonSlurper;
import org.apache.http.client.fluent.Request;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ArrayExampleTest {

    private static final String APPLICATION_JSON = "application/json";

    @Rule
    public PactProviderRuleMk2 provider = new PactProviderRuleMk2("ArrayExampleProvider", "localhost", 8082, this);

    @Pact(consumer = "ArrayExampleConsumer")
    public RequestResponsePact createFragment(PactDslWithProvider builder) {
        final DslPart body = new PactDslJsonBody()
                .eachArrayWithMinLike("foo", 3, 2)
                .object()
                .id()
                .stringType("bar")
                .closeObject()
                .closeArray()
                .closeArray();

        return builder.uponReceiving("a request expecting multiple items in an array")
                      .path("/")
                      .method("GET")
                      .willRespondWith()
                      .status(200)
                      .body(body)
                      .toPact();
    }

    @Test
    @PactVerification
    public void examplesAreGeneratedForArray() throws IOException {
        final String response = Request.Get("http://localhost:8082/")
                                       .addHeader("Accept", APPLICATION_JSON)
                                       .execute()
                                       .returnContent()
                                       .asString();

        final Map<String, Object> jsonResponse = (Map<String, Object>) new JsonSlurper().parseText(response);

        assertThat(jsonResponse, Matchers.hasKey("foo"));
        final List<List<Map<String, Object>>> fooArray = (List<List<Map<String, Object>>>) jsonResponse.get("foo");
        assertThat(fooArray.isEmpty(), is(false));
        assertThat(fooArray.size(), is(greaterThanOrEqualTo(2)));
        final List<Map<String, Object>> secondSubArray = fooArray.get(1);
        assertThat(secondSubArray.size(), is(1));
        final Map<String, Object> object = secondSubArray.get(0);
        assertThat(object, hasKey("id"));
        assertThat(object, hasKey("bar"));
        System.out.println(response);
    }
}
