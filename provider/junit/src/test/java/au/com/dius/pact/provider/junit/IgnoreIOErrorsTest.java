package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.target.Target;
import au.com.dius.pact.provider.junitsupport.target.TestTarget;
import org.junit.runner.RunWith;

@RunWith(PactRunner.class)
@Provider("myAwesomeService")
@IgnoreNoPactsToVerify(ignoreIoErrors = "true")
@PactBroker(host="pact-broker.net.doesnotexist")
public class IgnoreIOErrorsTest {
    @TestTarget
    public final Target target = new HttpTarget(8332);
}
