package au.com.dius.pact.consumer;

import au.com.dius.pact.model.*;
import org.junit.Test;

import static au.com.dius.pact.consumer.ConsumerInteractionJavaDsl.pactVerified;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class ConsumerPactTest {
    protected abstract PactFragment createFragment(ConsumerPactBuilder.PactDslWithProvider builder);
    protected abstract String providerName();
    protected abstract String consumerName();

    protected abstract void runTest(String endpoint);

    @Test
    public void testPact() {
        PactFragment fragment = createFragment(ConsumerPactBuilder.consumer(consumerName()).hasPactWith(providerName()));

        int port = (int) MockProviderConfig.randomPort().get();
        final MockProviderConfig config = new MockProviderConfig(port, "localhost");

        PactVerification.VerificationResult result = fragment.runConsumer(config,
                new Runnable() {
                    public void run() {
                        try {
                            runTest(config.url());
                        } catch(Exception e) {
                            fail("error thrown"+e);
                        }
                    }
                });
        assertEquals(pactVerified, result);
    }
}
