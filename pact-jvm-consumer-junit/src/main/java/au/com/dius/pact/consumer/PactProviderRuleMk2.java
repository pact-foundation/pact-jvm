package au.com.dius.pact.consumer;

import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.core.model.PactSpecVersion;

/**
 * A junit rule that wraps every test annotated with {@link PactVerification}.
 * Before each test, a mock server will be setup at given port/host that will provide mocked responses for the given
 * provider. After each test, it will be teared down.
 *
 * If no host is given, it will default to 127.0.0.1. If no port is given, it will default to a random port.
 *
 * If you need to use HTTPS, use PactHttpsProviderRuleMk2
 */
public class PactProviderRuleMk2 extends BaseProviderRule {

    /**
     * Creates a mock provider by the given name
     * @param provider Provider name to mock
     * @param hostInterface Host interface to bind to. Defaults to 127.0.0.1
     * @param port Port to bind to. Defaults to zero, which will bind to a random port.
     * @param pactVersion Pact specification version
     * @param target Target test to apply this rule to.
     */
    public PactProviderRuleMk2(String provider, String hostInterface, Integer port, PactSpecVersion pactVersion, Object target) {
        super(target, provider, hostInterface, port, pactVersion);
    }

    /**
     * Creates a mock provider by the given name
     * @param provider Provider name to mock
     * @param hostInterface Host interface to bind to. Defaults to 127.0.0.1
     * @param port Port to bind to. Defaults to a random port.
     * @param target Target test to apply this rule to.
     */
    public PactProviderRuleMk2(String provider, String hostInterface, Integer port, Object target) {
        this(provider, hostInterface, port, PactSpecVersion.V3, target);
    }

    /**
     * Creates a mock provider by the given name. Binds to localhost and a random port.
     * @param provider Provider name to mock
     * @param target Target test to apply this rule to.
     */
    public PactProviderRuleMk2(String provider, Object target) {
        this(provider, MockProviderConfig.LOCALHOST, 0, PactSpecVersion.V3, target);
    }

    /**
     * Creates a mock provider by the given name. Binds to localhost and a random port.
     * @param provider Provider name to mock
     * @param target Target test to apply this rule to.
     */
    public PactProviderRuleMk2(String provider, PactSpecVersion pactSpecVersion, Object target) {
        this(provider, MockProviderConfig.LOCALHOST, 0, pactSpecVersion, target);
    }

}
