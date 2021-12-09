package au.com.dius.pact.consumer.junit;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.PactMismatchesException;
import au.com.dius.pact.consumer.PactTestExecutionContext;
import au.com.dius.pact.consumer.PactVerificationResult;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.support.MetricEvent;
import au.com.dius.pact.core.support.Metrics;
import org.junit.Test;

import java.io.IOException;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;

public abstract class ConsumerPactTest {

    protected abstract RequestResponsePact createPact(PactDslWithProvider builder);
    protected abstract String providerName();
    protected abstract String consumerName();

    protected abstract void runTest(MockServer mockServer, PactTestExecutionContext context) throws IOException;

    @Test
    public void testPact() throws Throwable {
        RequestResponsePact pact = createPact(ConsumerPactBuilder.consumer(consumerName()).hasPactWith(providerName()));
        final MockProviderConfig config = MockProviderConfig.createDefault(getSpecificationVersion());

        PactVerificationResult result = runConsumerTest(pact, config, (mockServer, context) -> {
          runTest(mockServer, context);
          return null;
        });

        Metrics.INSTANCE.sendMetrics(new MetricEvent.ConsumerTestRun(pact.getInteractions().size(), "junit"));

        if (!(result instanceof PactVerificationResult.Ok)) {
            if (result instanceof PactVerificationResult.Error) {
              PactVerificationResult.Error error = (PactVerificationResult.Error) result;
              if (!(error.getMockServerState() instanceof PactVerificationResult.Ok)) {
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
