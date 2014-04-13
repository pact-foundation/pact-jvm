package au.com.dius.pact.consumer;

import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.Pact;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ConsumerPactTest extends AbstractConsumerPactTest {
    @Override
    protected Interaction createInteraction(ConsumerInteractionJavaDsl builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testreqheader", "testreqheadervalue");

        return builder.given("test state")
                .uponReceiving(
                        "java test interaction",
                        "GET",
                        "/",
                        headers,
                        "{\"test\":true}")
                .willRespondWith(200,
                        headers,
                        "{\"responsetest\":true}");
    }

    @Override
    protected String providerName() {
        return "test_provider";
    }

    @Override
    protected String consumerName() {
        return "test_consumer";
    }

    @Override
    protected void runTest(String url) {
        try {
            Future future = new Fixtures.ConsumerService(url).hitEndpoint();
            Object result = Await.result(future, Duration.apply(1, "s"));
            assertEquals(true, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
