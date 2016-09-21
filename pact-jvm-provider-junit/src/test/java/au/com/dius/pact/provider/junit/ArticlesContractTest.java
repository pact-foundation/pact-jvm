package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.junit.runner.RunWith;

@RunWith(PactRunner.class)
@Provider("ArticlesProvider")
@PactFolder("../pact-jvm-consumer-junit/build/2.11/pacts")
public class ArticlesContractTest {
    @TestTarget
    public final Target target = new HttpTarget(8000);
}
