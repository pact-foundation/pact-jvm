package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.PactFragment;
import au.com.dius.pact.core.model.PactSpecVersion;
import org.junit.Test;

import java.io.IOException;

/**
 * @deprecated Use ConsumerPactTestMk2 which uses the new mock server implementation
 */
@Deprecated
public abstract class ConsumerPactTest {
    public static final VerificationResult PACT_VERIFIED = PactVerified$.MODULE$;

    protected abstract PactFragment createFragment(PactDslWithProvider builder);
    protected abstract String providerName();
    protected abstract String consumerName();

    protected abstract void runTest(String url) throws IOException;

    @Test
    public void testPact() throws Throwable {
        PactFragment fragment = createFragment(ConsumerPactBuilder.consumer(consumerName()).hasPactWith(providerName()));
        final MockProviderConfig config = MockProviderConfig.createDefault(getSpecificationVersion());

        VerificationResult result = fragment.runConsumer(config, config1 -> runTest(config1.url()));

        if (!result.equals(PACT_VERIFIED)) {
            if (result instanceof PactError) {
                throw ((PactError)result).error();
            }
            if (result instanceof UserCodeFailed) {
                throw ((UserCodeFailed<RuntimeException>)result).error();
            }
            if (result instanceof PactMismatch) {
                PactMismatch mismatch = (PactMismatch) result;
                throw new PactMismatchException(mismatch);
            }
        }
    }

    protected PactSpecVersion getSpecificationVersion() {
        return PactSpecVersion.V3;
    }

}
