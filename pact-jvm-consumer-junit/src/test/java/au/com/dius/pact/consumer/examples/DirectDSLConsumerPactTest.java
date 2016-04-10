package au.com.dius.pact.consumer.examples;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.ConsumerPactTest;
import au.com.dius.pact.consumer.PactError;
import au.com.dius.pact.consumer.TestRun;
import au.com.dius.pact.consumer.VerificationResult;
import au.com.dius.pact.consumer.exampleclients.ProviderClient;
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.MockProviderConfig$;
import au.com.dius.pact.model.PactFragment;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Sometimes it is not convenient to use the ConsumerPactTest as it only allows one test per test class.
 * The DSL can be used directly in this case.
 */
public class DirectDSLConsumerPactTest {

    @Test
    public void testPact() {
        PactFragment pactFragment = ConsumerPactBuilder
                .consumer("Some Consumer")
                .hasPactWith("Some Provider")
                .uponReceiving("a request to say Hello")
                .path("/hello")
                .method("POST")
                .body("{\"name\": \"harry\"}")
                .willRespondWith()
                .status(200)
                .body("{\"hello\": \"harry\"}")
                .toFragment();

        MockProviderConfig config = MockProviderConfig$.MODULE$.createDefault();
        VerificationResult result = pactFragment.runConsumer(config, new TestRun() {
            @Override
            public void run(MockProviderConfig config) {
                Map expectedResponse = new HashMap();
                expectedResponse.put("hello", "harry");
                try {
                    assertEquals(new ProviderClient(config.url()).hello("{\"name\": \"harry\"}"),
                            expectedResponse);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        if (result instanceof PactError) {
            throw new RuntimeException(((PactError)result).error());
        }

        assertEquals(ConsumerPactTest.PACT_VERIFIED, result);
    }

}
