package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactFragment;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class Defect221Test {

    private static final String APPLICATION_JSON = "application/json";
    @Rule
    public PactProviderRule provider = new PactProviderRule("221_provider", "localhost", 8080, this);

    @Pact(provider="221_provider", consumer="test_consumer")
    public PactFragment createFragment(PactDslWithProvider builder) {
        return builder
            .given("test state")
            .uponReceiving("A request with double precision number")
                .path("/numbertest")
                .method("PUT")
                .body("{\"name\": \"harry\",\"data\": 1234.0 }", APPLICATION_JSON)
            .willRespondWith()
                .status(200)
                .body("{\"responsetest\": true, \"name\": \"harry\",\"data\": 1234.0 }", APPLICATION_JSON)
            .toFragment();
    }

    @Test
    @PactVerification("221_provider")
    public void runTest() throws IOException {
        assertEquals(Request.Put("http://localhost:8080/numbertest")
            .addHeader("Accept", APPLICATION_JSON)
            .bodyString("{\"name\": \"harry\",\"data\": 1234.0 }", ContentType.APPLICATION_JSON)
            .execute().returnContent().asString(), "{\"responsetest\": true, \"name\": \"harry\",\"data\": 1234.0 }");
    }
}
