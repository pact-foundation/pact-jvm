package com.dius.pact.consumer;

import com.dius.pact.author.PactServerConfig;
import com.dius.pact.model.MakePact;
import com.dius.pact.model.Pact;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.dius.pact.consumer.ConsumerPactJavaDsl.*;
import static com.dius.pact.consumer.ConsumerInteractionJavaDsl.*;

public class ConsumerPactTest {
    @Test
    public void testPact() {
        Map<String, String> headers = new HashMap<>();

        headers.put("testreqheader", "testreqheadervalue");

        Pact pact = makePact()
            .withProvider("test_provider")
            .withConsumer("test_consumer")
            .withInteractions(
                given("test state")
                    .uponReceiving(
                        "test interaction",
                        "/",
                        "GET",
                        headers,
                        "{\"test\":true}")
                    .willRespondWith(200,
                        headers,
                        "{\"responsetest\":true}")
            );

        PactServerConfig config = new PactServerConfig(9989, "localhost");

        Runnable test = new Runnable() {
            public void run() {
                assert true;
            }
        };

        new ConsumerPact(pact).runConsumer(config, "test state", test);
    }
}
