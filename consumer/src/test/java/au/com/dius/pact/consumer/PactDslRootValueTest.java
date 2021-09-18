package au.com.dius.pact.consumer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.dius.pact.core.model.PactSpecVersion;
import org.junit.Assert;
import org.junit.Test;

import au.com.dius.pact.consumer.dsl.PactDslRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.matchingrules.MatchingRule;
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup;
import au.com.dius.pact.core.model.matchingrules.RegexMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PactDslRootValueTest {

    @Test
    public void rootValueTest() {
        PactDslRootValue rootValueBody = new PactDslRootValue();

        rootValueBody.setValue("Brent");
        rootValueBody.setMatcher(new RegexMatcher(".{5}"));

        PactDslWithProvider dsl = new PactDslWithProvider(new ConsumerPactBuilder("consumer"), "provider");

        Map<String, String> headers = new HashMap<String, String>() {{
            put("Content-Type", "text/plain");
        }};

        RequestResponsePact frag = dsl
            .given("I am testing root values")
            .uponReceiving("A request for text/plain")
                .path("/some/blah/path")
                .headers(headers)
            .willRespondWith()
                .headers(headers)
                .status(200)
                .body(rootValueBody)
            .toPact().asRequestResponsePact().component1();

        Assert.assertEquals(1, frag.getInteractions().size());
        assertThat(frag.getInteractions().get(0).asSynchronousRequestResponse().getResponse().getBody().valueAsString(), is("Brent"));

        Map<String, MatchingRuleGroup> matchingGroups = frag.getInteractions()
                .get(0)
                .asSynchronousRequestResponse()
                .getResponse()
                .getMatchingRules()
                .rulesForCategory("body")
                .getMatchingRules();

        List<MatchingRule> rules = matchingGroups.get("$").getRules();
        Assert.assertEquals(1, rules.size());
        Assert.assertEquals(".{5}", rules.get(0).toMap(PactSpecVersion.V3).get("regex"));
    }
}
