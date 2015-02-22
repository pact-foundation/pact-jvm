package au.com.dius.pact.consumer;
import static org.junit.Assert.assertEquals;




import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;






import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.ConsumerPactBuilder.PactDslWithProvider.PactDslWithState;
import au.com.dius.pact.consumer.PactError;
import au.com.dius.pact.consumer.PactVerified$;
import au.com.dius.pact.consumer.TestRun;
import au.com.dius.pact.consumer.VerificationResult;
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.PactFragment;

/**
 * a junit rule that wraps every test annotated with {@link PactVerification}.
 * Before each test, a mock server will be setup at given port/host that will provide mocked responses.
 * after each test, it will be teared down.
 *
 */
public class PactRule extends ExternalResource {

    public static VerificationResult PACT_VERIFIED = PactVerified$.MODULE$;

    private Map <String, PactFragment> fragments;
    private Object target;
    final MockProviderConfig config;


    public PactRule(String host, int port, Object target) {
        config = new MockProviderConfig(port, host);
        this.target = target;
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
                if (fragment == null)
                    throw new UnsupportedOperationException("Fragment not found: " + pactDef.value());

                VerificationResult result = fragment.runConsumer(config, new TestRun() {

                    @Override
                    public void run(MockProviderConfig config) {
                        try {
                            base.evaluate();
                        } catch (Throwable e) {
                            fail("error thrown: "+e);
                        }
                    }
                });

                 if (result instanceof PactError) {
                    throw new RuntimeException(((PactError)result).error());
                }

                //writes all pacts to json files
                assertEquals(PACT_VERIFIED, result);
            }
        };
    }

    /**
     * scann all methods for @Pact annotation and execute them, if not already initialized
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
                        e.printStackTrace();
                    }
                }
            }

        }
        return fragments;
    }

    /**
     * validates method signature as described at {@link Pact}
     *
     */
    private boolean conformsToSigniture(Method m) {
        Pact pact = m.getAnnotation(Pact.class);
        boolean conforms =
            pact != null
            && PactFragment.class.isAssignableFrom(m.getReturnType())
            && m.getParameterTypes().length == 1
            && m.getParameterTypes()[0].isAssignableFrom(PactDslWithState.class);

        if (!conforms && pact != null) {
            throw new UnsupportedOperationException("Method "+m.getName()+" does not conform required method signature " + "'public PactFragment xxx(PactDslWithState builder)'");
        }
        return conforms;
    }

}
