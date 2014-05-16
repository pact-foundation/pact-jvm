package au.com.dius.pact.consumer;

import static au.com.dius.pact.consumer.ConsumerInteractionJavaDsl.PACT_VERIFIED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.Pact;

public abstract class AbstractConsumerPactTest {
    protected abstract Interaction createInteraction(ConsumerInteractionJavaDsl builder);
    protected abstract String providerName();
    protected abstract String consumerName();

    private Pact createPact() {
        return ConsumerPactJavaDsl.makePact()
                .withConsumer(consumerName())
                .withProvider(providerName())
                .withInteractions(createInteraction(new ConsumerInteractionJavaDsl()));
    }

    protected abstract void runTest(String endpoint);

    @Test
    public void testPact() {
        Pact pact = createPact();
        final MockProvider server = DefaultMockProvider.withDefaultConfig();
        ConsumerPactRunner runner = new ConsumerPactRunner(server);

        VerificationResult result = runner.runAndWritePact(pact,
            new Runnable() {
                public void run() {
                    try {
                        runTest(server.config().url());
                    } catch(Exception e) {
                        fail("error thrown"+e);
                    }
                }
            });
        assertEquals(PACT_VERIFIED, result);
    }
}
