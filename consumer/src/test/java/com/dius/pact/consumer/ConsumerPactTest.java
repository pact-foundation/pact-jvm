package com.dius.pact.consumer;

import akka.actor.ActorSystem;
import com.dius.pact.author.Fixtures;
import com.dius.pact.author.PactServerConfig;
import com.dius.pact.model.Pact;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

import static com.dius.pact.consumer.ConsumerPactJavaDsl.*;
import static com.dius.pact.consumer.ConsumerInteractionJavaDsl.*;

public class ConsumerPactTest {

    @Test
    public void testPact() {
        Map<String, String> headers = new HashMap<String, String>();

        headers.put("testreqheader", "testreqheadervalue");

        Pact pact = makePact()
            .withProvider("test_provider")
            .withConsumer("test_java_consumer")
            .withInteractions(
                given("test state")
                    .uponReceiving(
                        "java test interaction",
                        "/",
                        "GET",
                        headers,
                        "{\"test\":true}")
                    .willRespondWith(200,
                        headers,
                        "{\"responsetest\":true}")
            );

        final PactServerConfig config = new PactServerConfig(9989, "localhost");


        PactVerification.VerificationResult result = new ConsumerPact(pact).runConsumer(config, "test state",
            new Runnable() {
                public void run() {
                    ActorSystem system = ActorSystem.create("testServiceSystem");
                    Future future = new Fixtures.ConsumerService(config.url(), system).hitEndpoint();
                    try {
                        Object result = Await.result(future, Duration.apply(1, "s"));
                        assertEquals(true, result);
                    } catch(Exception e) {
                        fail("error thrown"+e);
                    }
                }
            });
        assertEquals(pactVerified, result);
    }
}
