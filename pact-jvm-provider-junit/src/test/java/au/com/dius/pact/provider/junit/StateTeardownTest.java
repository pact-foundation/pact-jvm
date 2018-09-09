package au.com.dius.pact.provider.junit;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static org.junit.Assert.assertEquals;

import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.github.restdriver.clientdriver.ClientDriverRule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

@RunWith(PactRunner.class)
@Provider("providerTeardownTest")
@PactFolder("pacts")
public class StateTeardownTest {

    private static Collection<Object> stateNameParamValues = new ArrayList<>();

    @ClassRule
    public static final ClientDriverRule embeddedProvider = new ClientDriverRule(8333);

    @TestTarget
    public final Target target = new HttpTarget(8333);

    @State(value = {"state 1", "state 2"}, action = StateChangeAction.SETUP)
    public void toDefaultState() {
        embeddedProvider.addExpectation(
            onRequestTo("/data"), giveEmptyResponse()
        );
    }

    @State(value = "state 1", action = StateChangeAction.TEARDOWN)
    public void teardownDefaultState(Map<String, Object> params) {
        stateNameParamValues.addAll(params.values());
    }

    @After
    public void after() {
        embeddedProvider.reset();
    }

    @AfterClass
    public static void assertTeardownWasCalledForState1() {
        assertEquals(Collections.singletonList("state 1"), stateNameParamValues);
    }

}
