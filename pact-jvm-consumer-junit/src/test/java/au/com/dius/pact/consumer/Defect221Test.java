package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactFragment;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class Defect221Test {

    @Rule
    public PactProviderRule provider = new PactProviderRule("221_provider", "localhost", 8080, this);

    @Pact(provider="221_provider", consumer="test_consumer")
    public PactFragment createFragment(PactDslWithProvider builder) {
        return builder
            .given("test state")
            .uponReceiving("A request with double precision number")
                .path("/numbertest")
                .method("PUT")
                .body("{\"name\": \"harry\",\"data\": 1234.0 }", "application/json")
            .willRespondWith()
                .status(200)
                .body("{\"responsetest\": true, \"name\": \"harry\",\"data\": 1234.0 }", "application/json")
            .toFragment();
    }

    @Test
    @PactVerification("221_provider")
    public void runTest() throws IOException {
        assertEquals(Request.Put("http://localhost:8080/numbertest")
            .addHeader("Accept", "application/json")
            .bodyString("{\"name\": \"harry\",\"data\": 1234.0 }", ContentType.APPLICATION_JSON)
            .execute().returnContent().asString(), "{\"responsetest\": true, \"name\": \"harry\",\"data\": 1234.0 }");
    }
}
