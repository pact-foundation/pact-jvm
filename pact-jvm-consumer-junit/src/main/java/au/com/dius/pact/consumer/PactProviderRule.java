package au.com.dius.pact.consumer;

import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.PactFragment;
import org.apache.commons.lang3.StringUtils;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A junit rule that wraps every test annotated with {@link PactVerification}.
 * Before each test, a mock server will be setup at given port/host that will provide mocked responses for the given
 * provider. After each test, it will be teared down.
 *
 * If no host is given, it will default to localhost. If no port is given, it will default to a random port.
 */
public class PactProviderRule extends ExternalResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PactProviderRule.class);

    public static VerificationResult PACT_VERIFIED = PactVerified$.MODULE$;

    private Map <String, PactFragment> fragments;
    private final String provider;
    private Object target;
    private final MockProviderConfig config;

    /**
     * Creates a mock provider by the given name
     * @param provider Provider name to mock
     * @param host Host to bind to. Defaults to localhost
     * @param port Port to bind to. Defaults to a random port.
     * @param target Target test to apply this rule to.
     */
    public PactProviderRule(String provider, String host, Integer port, Object target) {
        this.provider = provider;
        this.target = target;
        if (host == null && port == null) {
            config = MockProviderConfig.createDefault();
        } else {
            config = new MockProviderConfig(port, host);
        }
    }

    /**
     * Creates a mock provider by the given name. Binds to localhost and a random port.
     * @param provider Provider name to mock
     * @param target Target test to apply this rule to.
     */
    public PactProviderRule(String provider, Object target) {
        this(provider, null, null, target);
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
                // no pactVerification? execute the test normally
                if (pactDef == null) {
                    base.evaluate();
                    return;
                }

                Map<String, PactFragment> pacts = getPacts();
                Optional<PactFragment> fragment;
                if (pactDef.value().length == 1 && StringUtils.isEmpty(pactDef.value()[0])) {
                    fragment = pacts.values().stream().findFirst();
                } else {
                    fragment = Arrays.asList(pactDef.value()).stream().map(pacts::get)
                            .filter(p -> p != null).findFirst();
                }
                if (!fragment.isPresent()) {
                    base.evaluate();
                    return;
                }

                VerificationResult result = fragment.get().runConsumer(config, new TestRun() {
                    @Override
                    public void run(MockProviderConfig config) throws Throwable {
                        base.evaluate();
                    }
                });

                if (!result.equals(PACT_VERIFIED)) {
                    if (result instanceof PactError) {
                        throw ((PactError)result).error();
                    }
                    if (result instanceof UserCodeFailed) {
                        throw ((UserCodeFailed<RuntimeException>)result).error();
                    }
                    if (result instanceof PactMismatch && !pactDef.expectMismatch()) {
                        PactMismatch mismatch = (PactMismatch) result;
                        throw new PactMismatchException(mismatch);
                    }
                } else if (pactDef.expectMismatch()) {
                    throw new RuntimeException("Expected a pact mismatch (PactVerification.expectMismatch is set to true)");
                }
            }
        };
    }

    /**
     * scan all methods for @Pact annotation and execute them, if not already initialized
     */
    protected Map<String, PactFragment> getPacts() {
        if (fragments == null) {
            fragments = new HashMap <String, PactFragment> ();
            for (Method m: target.getClass().getMethods()) {
                if (conformsToSignature(m)) {
                    Pact pact = m.getAnnotation(Pact.class);
                    if (StringUtils.isEmpty(pact.provider()) || provider.equals(pact.provider())) {
                        ConsumerPactBuilder.PactDslWithProvider dslBuilder = ConsumerPactBuilder.consumer(pact.consumer())
                            .hasPactWith(provider);
                        try {
                            fragments.put(provider, (PactFragment) m.invoke(target, dslBuilder));
                        } catch (Exception e) {
                            LOGGER.error("Failed to invoke pact method", e);
                            throw new RuntimeException("Failed to invoke pact method", e);
                        }
                    }
                }
            }
        }
        return fragments;
    }

    /**
     * validates method signature as described at {@link Pact}
     */
    private boolean conformsToSignature(Method m) {
        Pact pact = m.getAnnotation(Pact.class);
        boolean conforms =
            pact != null
            && PactFragment.class.isAssignableFrom(m.getReturnType())
            && m.getParameterTypes().length == 1
            && m.getParameterTypes()[0].isAssignableFrom(ConsumerPactBuilder.PactDslWithProvider.class);

        if (!conforms && pact != null) {
            throw new UnsupportedOperationException("Method " + m.getName() +
                " does not conform required method signature 'public PactFragment xxx(PactDslWithProvider builder)'");
        }
        return conforms;
    }

}
