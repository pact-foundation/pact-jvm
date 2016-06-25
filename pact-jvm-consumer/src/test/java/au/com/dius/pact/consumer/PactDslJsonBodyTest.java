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

    private static final String NUMBERS = "numbers";
    private static final String K_DEPRECIATION_BIPS = "10k-depreciation-bips";
    private static final String FIRST = "first";
    private static final String LEVEL_1 = "level1";
    private static final String L_1_EXAMPLE = "l1example";
    private static final String SECOND = "second";
    private static final String LEVEL_2 = "level2";
    private static final String L_2_EXAMPLE = "l2example";
    private static final String THIRD = "@third";

    @Test
    public void guardAgainstObjectNamesThatDontConformToGatlingFields() {
        DslPart body = new PactDslJsonBody()
            .id()
            .object("2")
                .id()
                .stringValue("test", "A Test String")
            .closeObject()
            .array(NUMBERS)
                .id()
                .number(100)
                .numberValue(101)
                .hexValue()
                .object()
                    .id()
                    .stringValue("name", "Rogger the Dogger")
                    .timestamp()
                    .date("dob", "MM/dd/yyyy")
                    .object(K_DEPRECIATION_BIPS)
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
                new HashSet(Arrays.asList("2", NUMBERS, "id")))));
    }

    @Test
    public void guardAgainstFieldNamesThatDontConformToGatlingFields() {
        DslPart body = new PactDslJsonBody()
                .id("1")
                .stringType("@field")
                .hexValue("200", "abc")
                .integerType(K_DEPRECIATION_BIPS);

        Set<String> expectedMatchers = new HashSet<String>(Arrays.asList(
                "['200']", "['1']", "['@field']", "['10k-depreciation-bips']"
        ));
        assertThat(body.getMatchers().keySet(), is(equalTo(expectedMatchers)));

        assertThat(((JSONObject) body.getBody()).keySet(), is(equalTo((Set)
                new HashSet(Arrays.asList("200", K_DEPRECIATION_BIPS, "1", "@field")))));
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
                .object(FIRST)
                .stringType(LEVEL_1, L_1_EXAMPLE)
                .stringType("@level1")
                .object(SECOND)
                .stringType(LEVEL_2, L_2_EXAMPLE)
                .object(THIRD)
                .stringType("level3", "l3example")
                .object("fourth")
                .stringType("level4", "l4example")
                .closeObject()
                .closeObject()
                .closeObject()
                .closeObject();

        Set<String> expectedMatchers = new HashSet<>(Arrays.asList(
                ".first.second['@third'].fourth.level4",
                ".first.second['@third'].level3",
                ".first.second.level2",
                ".first.level1",
                ".first['@level1']"
        ));

        assertThat(body.getMatchers().keySet(), is(equalTo(expectedMatchers)));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject(FIRST)
                .getString(LEVEL_1), is(equalTo(L_1_EXAMPLE)));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject(FIRST)
                .getJSONObject(SECOND)
                .getString(LEVEL_2), is(equalTo(L_2_EXAMPLE)));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject(FIRST)
                .getJSONObject(SECOND)
                .getJSONObject(THIRD)
                .getString("level3"), is(equalTo("l3example")));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject(FIRST)
                .getJSONObject(SECOND)
                .getJSONObject(THIRD)
                .getJSONObject("fourth")
                .getString("level4"), is(equalTo("l4example")));
    }

    @Test
    public void nestedArrayMatcherTest() {
        DslPart body = new PactDslJsonBody()
                .array(FIRST)
                .stringType(L_1_EXAMPLE)
                .array()
                .stringType(L_2_EXAMPLE)
                .closeArray()
                .closeArray();

        Set<String> expectedMatchers = new HashSet<String>(Arrays.asList(
                ".first[0]",
                ".first[1][0]"
        ));

        assertThat(body.getMatchers().keySet(), is(equalTo(expectedMatchers)));

        assertThat(((JSONObject)body.getBody())
                .getJSONArray(FIRST)
                .getString(0), is(equalTo(L_1_EXAMPLE)));

        assertThat(((JSONObject)body.getBody())
                .getJSONArray(FIRST)
                .getJSONArray(1)
                .getString(0), is(equalTo(L_2_EXAMPLE)));
    }

    @Test
    public void nestedArrayAndObjectMatcherTest() {
        DslPart body = new PactDslJsonBody()
                .object(FIRST)
                .stringType(LEVEL_1, L_1_EXAMPLE)
                .array(SECOND)
                .stringType("al2example")
                .object()
                .stringType(LEVEL_2, L_2_EXAMPLE)
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
                .getJSONObject(FIRST)
                .getString(LEVEL_1), is(equalTo(L_1_EXAMPLE)));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject(FIRST)
                .getJSONArray(SECOND)
                .getString(0), is(equalTo("al2example")));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject(FIRST)
                .getJSONArray(SECOND)
                .getJSONObject(1)
                .getString(LEVEL_2), is(equalTo(L_2_EXAMPLE)));

        assertThat(((JSONObject)body.getBody())
                .getJSONObject(FIRST)
                .getJSONArray(SECOND)
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
          .array(NUMBERS)
            .id()
            .nullValue()
            .stringValue(null)
          .closeArray();

        JSONObject jsonObject = (JSONObject) body.getBody();
        assertThat(jsonObject.keySet(), is(equalTo((Set) new HashSet(Arrays.asList("2", NUMBERS, "id")))));

        assertThat(jsonObject.getJSONObject("2").get("test"), is(JSONObject.NULL));
        JSONArray numbers = jsonObject.getJSONArray(NUMBERS);
        assertThat(numbers.length(), is(3));
        assertThat(numbers.get(0), is(notNullValue()));
        assertThat(numbers.get(1), is(JSONObject.NULL));
        assertThat(numbers.get(2), is(JSONObject.NULL));
    }
}
