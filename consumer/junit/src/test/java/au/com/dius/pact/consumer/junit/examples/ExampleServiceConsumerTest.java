package au.com.dius.pact.consumer.junit.examples;

import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.RequestResponsePact;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Example taken from https://groups.google.com/forum/#!topic/pact-support/-Kk_OxvcJQY
 */
public class ExampleServiceConsumerTest {

    String DATA_A_ID = "AAAAAAAA_ID";
    String DATA_B_ID = "BBBBBBBB_ID";

    Map<String, String> headers = MapUtils.putAll(new HashMap<String, String>(),
        new String[]{"Content-Type", "application/json;charset=UTF-8"});

    @Rule
    public PactProviderRule provider = new PactProviderRule("CarBookingProvider", this);

    @Pact(provider = "CarBookingProvider", consumer = "CarBookingConsumer")
    public RequestResponsePact configurationFragment(PactDslWithProvider builder) {
        return builder
            .given("john smith books a civic")
            .uponReceiving("retrieve data from Service-A")
            .path("/persons/" + DATA_A_ID)
            .method("GET")
            .willRespondWith()
            .headers(headers)
            .status(200)
            .body(
                new PactDslJsonBody()
                    .stringValue("id", DATA_A_ID)
                    .stringValue("firstName", "John")
                    .stringValue("lastName", "Smith")
            )

            .uponReceiving("retrieving data from Service-B")
            .path("/cars/" + DATA_B_ID)
            .method("GET")
            .willRespondWith()
            .headers(headers)
            .status(200)
            .body(
                new PactDslJsonBody()
                    .stringValue("id", DATA_B_ID)
                    .stringValue("brand", "Honda")
                    .stringValue("model", "Civic")
                    .numberValue("year", 2012)
            )

            .uponReceiving("book a car")
            .path("/orders/")
            .method("POST")
            .body(
                new PactDslJsonBody()
                    .object("person")
                        .stringValue("id", DATA_A_ID)
                        .stringValue("firstName", "John")
                        .stringValue("lastName", "Smith")
                    .closeObject()
                    .object("cars")
                        .stringValue("id", DATA_B_ID)
                        .stringValue("brand", "Honda")
                        .stringValue("model", "Civic")
                        .numberValue("year", 2012)
                    .closeObject()
            )
            .willRespondWith()
            .headers(headers)
            .status(201)
            .body(
                new PactDslJsonBody()
                    .stringMatcher("id", "ORDER_ID_\\d+", "ORDER_ID_123456")
            )
            .toPact();
    }

    @PactVerification("CarBookingProvider")
    @Test
    public void testBookCar() throws IOException {
        ProviderCarBookingRestClient providerRestClient = new ProviderCarBookingRestClient();
        HttpResponse response = providerRestClient.placeOrder(provider.getUrl(), DATA_A_ID, DATA_B_ID, "2015-03-15");

        Assert.assertEquals(201, response.getStatusLine().getStatusCode());
        String orderDetails = EntityUtils.toString(response.getEntity());
        Assert.assertEquals("{\"id\":\"ORDER_ID_123456\"}", orderDetails);
    }
}
