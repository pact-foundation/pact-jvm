package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.PactSpecVersion;
import au.com.dius.pact.model.RequestResponsePact;
import org.junit.Test;

import java.io.IOException;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;

public abstract class ConsumerPactTestMk2 {

    protected abstract RequestResponsePact createPact(PactDslWithProvider builder);
    protected abstract String providerName();
    protected abstract String consumerName();

    protected abstract void runTest(MockServer mockServer, PactTestExecutionContext context) throws IOException;

    @Test
    public void testPact() throws Throwable {
        RequestResponsePact pact = createPact(ConsumerPactBuilder.consumer(consumerName()).hasPactWith(providerName()));
        final MockProviderConfig config = MockProviderConfig.createDefault(getSpecificationVersion());

        PactVerificationResult result = runConsumerTest(pact, config, this::runTest);

        if (!result.equals(PactVerificationResult.Ok.INSTANCE)) {
            if (result instanceof PactVerificationResult.Error) {
              PactVerificationResult.Error error = (PactVerificationResult.Error) result;
              if (error.getMockServerState() != PactVerificationResult.Ok.INSTANCE) {
                throw new AssertionError("Pact Test function failed with an exception, possibly due to " +
                  error.getMockServerState(), ((PactVerificationResult.Error) result).getError());
              } else {
                throw new AssertionError("Pact Test function failed with an exception: " +
                  error.getError().getMessage(), error.getError());
              }
            } else {
                throw new PactMismatchesException(result);
            }
        }
    }

    protected PactSpecVersion getSpecificationVersion() {
        return PactSpecVersion.V3;
    }

}
