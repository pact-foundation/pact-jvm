package au.com.dius.pact.provider.maven

import org.junit.Before
import org.junit.Test

class PactProviderMojoTest {

    private PactProviderMojo mojo
    private Consumer consumer

    @Before
    void setup() {
        mojo = new PactProviderMojo()
        consumer = new Consumer(name: 'Test Consumer')
    }

    @Test
    void 'property is defined if there is a system property with that key'() {
        System.setProperty('pact.test.100', '100')
        assert mojo.propertyDefined('pact.test.100')
    }

    @Test
    void 'property is defined if there is a configuration property with that key'() {
        mojo.configuration['pact.test.101'] = '101'
        assert mojo.propertyDefined('pact.test.101')
    }

    @Test
    void 'property is not defined if there is no system or configuration property with that key'() {
        assert !mojo.propertyDefined('pact.test.102')
    }

    @Test
    void 'property method returns system property'() {
        System.setProperty('pact.test.103', '100')
        assert mojo.property('pact.test.103') == '100'
    }

    @Test
    void 'property defaults to configuration property if there is no system property'() {
        mojo.configuration['pact.test.104'] = '101'
        assert mojo.property('pact.test.104') == '101'
    }

    @Test
    void 'property method returns null if there is no system or configuration property with that key'() {
        assert mojo.property('pact.test.105') == null
    }

}
