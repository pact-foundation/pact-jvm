package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import au.com.dius.pact.provider.junitsupport.target.Target;
import au.com.dius.pact.provider.junitsupport.target.TestTarget;
import org.junit.AfterClass;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(PactRunner.class)
@Provider("myAwesomeService")
@PactBroker(url = "http://broker.host")
@IgnoreNoPactsToVerify(ignoreIoErrors = "true")
public class ConsumerVersionSelectorTest {
    @TestTarget
    public final Target target = new HttpTarget(8332);

    static boolean called = false;
    @PactBrokerConsumerVersionSelectors
    public static SelectorBuilder consumerVersionSelectors() {
        called = true;
        return new SelectorBuilder().branch("current");
    }

    @AfterClass
    public static void after() {
        assertThat("consumerVersionSelectors() was not called", called, is(true));
    }
}
