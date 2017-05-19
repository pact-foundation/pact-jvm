package au.com.dius.pact.consumer;

import au.com.dius.pact.model.MockHttpsProviderConfig;
import au.com.dius.pact.model.PactSpecVersion;

/**
 * A junit rule that wraps every test annotated with {@link PactVerification}.
 * Before each test, a mock server will be setup at given port/host that will provide mocked responses for the given
 * provider. After each test, it will be teared down.
 *
 * If no host is given, it will default to 127.0.0.1. If no port is given, it will default to a random port.
 */
public class PactHttpsProviderRuleMk2 extends BaseProviderRule {

    /**
     * Creates a mock provider by the given name
     * @param provider Provider name to mock
     * @param hostInterface Host to bind to. Defaults to localhost
     * @param port Port to bind to. Defaults to a random port.
     * @param pactVersion Pact specification version
     * @param target Target test to apply this rule to.
     */
    public PactHttpsProviderRuleMk2(String provider, String hostInterface, Integer port, PactSpecVersion pactVersion, Object target) {
        super(target, provider, hostInterface, port, pactVersion);
    }

    /**
     * Creates a mock provider by the given name
     * @param provider Provider name to mock
     * @param host Host to bind to. Defaults to localhost
     * @param port Port to bind to. Defaults to a random port.
     * @param https Boolean flag to control starting HTTPS or HTTP mock server
     * @param pactVersion Pact specification version
     * @param target Target test to apply this rule to.
     */
    public PactHttpsProviderRuleMk2(String provider, String host, Integer port, boolean https, PactSpecVersion pactVersion,
                                    Object target) {
      this(provider, host, port, pactVersion, target);
      if (https) {
        config = MockHttpsProviderConfig.httpsConfig(host, port, pactVersion);
      }
    }

    /**
     * Creates a mock provider by the given name
     * @param provider Provider name to mock
     * @param host Host to bind to. Defaults to localhost
     * @param port Port to bind to. Defaults to a random port.
     * @param target Target test to apply this rule to.
     */
    public PactHttpsProviderRuleMk2(String provider, String host, Integer port, Object target) {
        this(provider, host, port, PactSpecVersion.V2, target);
    }

    /**
     * Creates a mock provider by the given name. Binds to localhost and a random port.
     * @param provider Provider name to mock
     * @param target Target test to apply this rule to.
     */
    public PactHttpsProviderRuleMk2(String provider, Object target) {
        this(provider, null, null, PactSpecVersion.V2, target);
    }

    /**
     * Creates a mock provider by the given name. Binds to localhost and a random port.
     * @param provider Provider name to mock
     * @param target Target test to apply this rule to.
     */
    public PactHttpsProviderRuleMk2(String provider, PactSpecVersion pactSpecVersion, Object target) {
        this(provider, null, null, pactSpecVersion, target);
    }
}
