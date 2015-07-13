package au.com.dius.pact.consumer;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class PactDslJsonBodyTest {

    @Test
    public void guardAgainstObjectNamesThatDontConformToGatlingFields() {
        DslPart body = new PactDslJsonBody()
            .id()
            .object("2")
                .id()
                .stringValue("test", "A Test String")
            .closeObject()
            .array("numbers")
                .id()
                .number(100)
                .numberValue(101)
                .hexValue()
                .object()
                    .id()
                    .stringValue("name", "Rogger the Dogger")
                    .timestamp()
                    .date("dob", "MM/dd/yyyy")
                    .object("1")
                        .id()
                    .closeObject()
                .closeObject()
            .closeArray();

        Set<String> expectedMatchers = new HashSet<String>(Arrays.asList(
                ".id",
                "['2'].id",
                ".numbers[3]",
                ".numbers[0]",
                ".numbers[4].timestamp",
                ".numbers[4].dob",
                ".numbers[4].id",
                ".numbers[4]['1'].id"
        ));
        assertThat(body.getMatchers().keySet(), is(equalTo(expectedMatchers)));

        assertThat(((JSONObject) body.getBody()).keySet(), is(equalTo((Set)
                new HashSet(Arrays.asList("2", "numbers", "id")))));
    }

    @Test
    public void guardAgainstFieldNamesThatDontConformToGatlingFields() {
        DslPart body = new PactDslJsonBody()
                .id("1")
                .stringType("@field")
                .hexValue("200", "abc");

        Set<String> expectedMatchers = new HashSet<String>(Arrays.asList(
                "['200']", "['1']", "['@field']"
        ));
        assertThat(body.getMatchers().keySet(), is(equalTo(expectedMatchers)));

        assertThat(((JSONObject) body.getBody()).keySet(), is(equalTo((Set)
                new HashSet(Arrays.asList("200", "1", "@field")))));
    }

    @Test
    public void eachLikeMatcherTest() {
        DslPart body = new PactDslJsonBody()
                .eachLike("ids")
                    .id()
                    .closeObject()
                .closeArray();

        Set<String> expectedMatchers = new HashSet<String>(Arrays.asList(
                ".ids",
                ".ids[*].id"
        ));
        assertThat(body.getMatchers().keySet(), is(equalTo(expectedMatchers)));

        assertThat(((JSONObject) body.getBody()).keySet(), is(equalTo((Set)
                new HashSet(Arrays.asList("ids")))));
    }
}
