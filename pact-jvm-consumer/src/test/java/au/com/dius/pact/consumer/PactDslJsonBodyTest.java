package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
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
                    .object("10k-depreciation-bips")
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
                ".numbers[4]['10k-depreciation-bips'].id"
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
                .hexValue("200", "abc")
                .integerType("10k-depreciation-bips");

        Set<String> expectedMatchers = new HashSet<String>(Arrays.asList(
                "['200']", "['1']", "['@field']", "['10k-depreciation-bips']"
        ));
        assertThat(body.getMatchers().keySet(), is(equalTo(expectedMatchers)));

        assertThat(((JSONObject) body.getBody()).keySet(), is(equalTo((Set)
                new HashSet(Arrays.asList("200", "10k-depreciation-bips", "1", "@field")))));
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

    @Test
    public void nestedObjectMatcherTest() {
        DslPart body = new PactDslJsonBody()
                .object("first")
                .stringType("level1", "l1example")
                .object("second")
                .stringType("level2", "l2example")
                .object("@third")
                .stringType("level3", "l3example")
                .object("fourth")
                .stringType("level4", "l4example")
                .closeObject()
                .closeObject()
                .closeObject()
                .closeObject();

        Set<String> expectedMatchers = new HashSet<String>(Arrays.asList(
                ".first.second['@third'].fourth.level4",
                ".first.second['@third'].level3",
                ".first.second.level2",
                ".first.level1"
        ));

        assertThat(body.getMatchers().keySet(), is(equalTo(expectedMatchers)));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject("first")
                .getString("level1"), is(equalTo("l1example")));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject("first")
                .getJSONObject("second")
                .getString("level2"), is(equalTo("l2example")));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject("first")
                .getJSONObject("second")
                .getJSONObject("@third")
                .getString("level3"), is(equalTo("l3example")));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject("first")
                .getJSONObject("second")
                .getJSONObject("@third")
                .getJSONObject("fourth")
                .getString("level4"), is(equalTo("l4example")));
    }

    @Test
    public void nestedArrayMatcherTest() {
        DslPart body = new PactDslJsonBody()
                .array("first")
                .stringType("l1example")
                .array()
                .stringType("l2example")
                .closeArray()
                .closeArray();

        Set<String> expectedMatchers = new HashSet<String>(Arrays.asList(
                ".first[0]",
                ".first[1][0]"
        ));

        assertThat(body.getMatchers().keySet(), is(equalTo(expectedMatchers)));

        assertThat(((JSONObject)body.getBody())
                .getJSONArray("first")
                .getString(0), is(equalTo("l1example")));

        assertThat(((JSONObject)body.getBody())
                .getJSONArray("first")
                .getJSONArray(1)
                .getString(0), is(equalTo("l2example")));
    }

    @Test
    public void nestedArrayAndObjectMatcherTest() {
        DslPart body = new PactDslJsonBody()
                .object("first")
                .stringType("level1", "l1example")
                .array("second")
                .stringType("al2example")
                .object()
                .stringType("level2", "l2example")
                .array("third")
                .stringType("al3example")
                .closeArray()
                .closeObject()
                .closeArray()
                .closeObject();

        Set<String> expectedMatchers = new HashSet<String>(Arrays.asList(
                ".first.level1",
                ".first.second[1].level2",
                ".first.second[0]",
                ".first.second[1].third[0]"
        ));

        assertThat(body.getMatchers().keySet(), is(equalTo(expectedMatchers)));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject("first")
                .getString("level1"), is(equalTo("l1example")));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject("first")
                .getJSONArray("second")
                .getString(0), is(equalTo("al2example")));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject("first")
                .getJSONArray("second")
                .getJSONObject(1)
                .getString("level2"), is(equalTo("l2example")));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject("first")
                .getJSONArray("second")
                .getJSONObject(1)
                .getJSONArray("third")
                .getString(0), is(equalTo("al3example")));

    }

    @Test
    public void allowSettingFieldsToNull() {
        DslPart body = new PactDslJsonBody()
          .id()
          .object("2")
            .id()
            .stringValue("test", null)
            .nullValue("nullValue")
          .closeObject()
          .array("numbers")
            .id()
            .nullValue()
            .stringValue(null)
          .closeArray();

        JSONObject jsonObject = (JSONObject) body.getBody();
        assertThat(jsonObject.keySet(), is(equalTo((Set) new HashSet(Arrays.asList("2", "numbers", "id")))));

        assertThat(jsonObject.getJSONObject("2").get("test"), is(JSONObject.NULL));
        JSONArray numbers = jsonObject.getJSONArray("numbers");
        assertThat(numbers.length(), is(3));
        assertThat(numbers.get(0), is(notNullValue()));
        assertThat(numbers.get(1), is(JSONObject.NULL));
        assertThat(numbers.get(2), is(JSONObject.NULL));
    }
}
