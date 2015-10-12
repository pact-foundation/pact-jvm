package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslWithState;
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.MockProviderConfig$;
import au.com.dius.pact.model.PactConfig;
import au.com.dius.pact.model.PactFragment;
import au.com.dius.pact.model.PactSpecVersion;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A junit rule that wraps every test annotated with {@link PactVerification}.
 * Before each test, a mock server will be setup at given port/host that will provide mocked responses.
 * after each test, it will be teared down.
 *
 * If no host is given, it will default to localhost. If no port is given, it will default to a random port.
 *
 * @deprecated Use PactProviderRule instead
 */
@Deprecated
public class PactRule extends ExternalResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PactRule.class);

    public static VerificationResult PACT_VERIFIED = PactVerified$.MODULE$;

    private Map <String, PactFragment> fragments;
    private Object target;
    private final MockProviderConfig config;

    public PactRule(String host, int port, Object target) {
        config = new MockProviderConfig(port, host, PactConfig.apply(PactSpecVersion.V2));
        this.target = target;
    }

    public PactRule(String host, Object target) {
        config = MockProviderConfig$.MODULE$.createDefault(host, PactConfig.apply(PactSpecVersion.V2));
        this.target = target;
    }

    public PactRule(Object target) {
        config = MockProviderConfig$.MODULE$.createDefault(PactConfig.apply(PactSpecVersion.V2));
        this.target = target;
    }

    public MockProviderConfig getConfig() {
        return config;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                PactVerification pactDef = description.getAnnotation(PactVerification.class);
                //no pactVerification? execute the test normally
                if (pactDef == null) {
                    base.evaluate();
                    return;
                }

                PactFragment fragment = getPacts().get(pactDef.value());
                if (fragment == null) {
                    throw new UnsupportedOperationException("Fragment not found: " + pactDef.value());
                }

                VerificationResult result = fragment.runConsumer(config, new TestRun() {

                    @Override
                    public void run(MockProviderConfig config) throws Throwable {
                        base.evaluate();
                    }
                });

                if (!result.equals(PACT_VERIFIED)) {
                    if (result instanceof PactError) {
                        throw new RuntimeException(((PactError)result).error());
                    }
                    if (result instanceof UserCodeFailed) {
                        throw new RuntimeException(((UserCodeFailed<RuntimeException>)result).error());
                    }
                    if (result instanceof PactMismatch) {
                        PactMismatch mismatch = (PactMismatch) result;
                        throw new PactMismatchException(mismatch);
                    }
                }
            }
        };
    }

    /**
     * scan all methods for @Pact annotation and execute them, if not already initialized
     * @return
     */
    protected Map < String, PactFragment > getPacts() {
        if (fragments == null) {
            fragments = new HashMap <String, PactFragment> ();

            for (Method m: target.getClass().getMethods()) {
                if (conformsToSigniture(m)) {
                    Pact pact = m.getAnnotation(Pact.class);
                    PactDslWithState dslBuilder = ConsumerPactBuilder.consumer(pact.consumer())
                        .hasPactWith(pact.provider())
                        .given(pact.state());
                    try {
                        fragments.put(pact.state(), (PactFragment) m.invoke(target, dslBuilder));
                    } catch (Exception e) {
                        LOGGER.error("Failed to invoke pact method", e);
                        throw new RuntimeException("Failed to invoke pact method", e);
                    }
                }
            }

        }
        return fragments;
    }

    /**
     * validates method signature as described at {@link Pact}
     */
    private boolean conformsToSigniture(Method m) {
        Pact pact = m.getAnnotation(Pact.class);
        boolean conforms =
            pact != null
            && PactFragment.class.isAssignableFrom(m.getReturnType())
            && m.getParameterTypes().length == 1
            && m.getParameterTypes()[0].isAssignableFrom(PactDslWithState.class);

        if (!conforms && pact != null) {
            throw new UnsupportedOperationException("Method " + m.getName() +
                " does not conform required method signature 'public PactFragment xxx(PactDslWithState builder)'");
        }
        return conforms;
    }

}
