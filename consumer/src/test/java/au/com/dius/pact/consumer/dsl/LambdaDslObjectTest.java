package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PM;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.LambdaDslJsonBody;
import au.com.dius.pact.consumer.dsl.LambdaDslObject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LambdaDslObjectTest {

    @Test
    public void testStringValue() {
         /*
            {
                "bar": "Bar",
                "foo": "Foo"
            }
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .stringValue("foo", "Foo")
                .stringValue("bar", "Bar")
                .getBody().toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody("", "", null);
        final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
        object
                .stringValue("foo", "Foo")
                .stringValue("bar", "Bar");
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().isEmpty(), is(true));
    }

    @Test
    public void testNumberValue() {
         /*
            { "number": 1 }
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
          .numberValue("number", 1)
          .getBody().toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody("", "", null);
        final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
        object
          .numberValue("number", 1);
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().isEmpty(), is(true));
    }

    @Test
    public void testStringMatcher() {
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody("", "", null);
        final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
        object
                .stringMatcher("foo", "[a-z][0-9]");
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString().replace("\"", "'");
        assertThat(actualJson, containsString("{'foo':"));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(1));
        final Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("regex"));
        assertThat(matcher.get("regex"), is("[a-z][0-9]"));
    }

    @Test
    public void testStringMatcherWithExample() {
         /*
            {
                "foo": "a0"
            }
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .stringMatcher("foo", "[a-z][0-9]", "a0")
                .getBody().toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
        object
                .stringMatcher("foo", "[a-z][0-9]", "a0");
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(1));
        final Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("regex"));
        assertThat(matcher.get("regex"), is("[a-z][0-9]"));
    }

    @Test
    public void testStringType() {
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
        object
                .stringType("foo");
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString().replace("\"", "'");
        assertThat(actualJson, containsString("{'foo':"));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(1));
        final Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("type"));
    }

    @Test
    public void testStringTypeWithExample() {
        /*
            {
                "foo": "Foo"
            }
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .stringType("foo", "Foo")
                .getBody().toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody("", "", null);
        final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
        object
                .stringType("foo", "Foo");
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(1));
        final Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("type"));
    }

    @Test
    public void testStringTypes() {
        /*
            {
                "foo": [
                    {
                        "bar": "Bar"
                    }
                ]
            }
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .stringType(new String[]{"foo", "bar"})
                .getBody().toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
        object
                .stringType(new String[]{"foo", "bar"});
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        // It can't be equals because the values get generated randomly on each run
        //assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        assertThat(actualJson, containsString("foo"));
        assertThat(actualJson, containsString("bar"));
        Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("type"));
        matcher = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("type"));
    }

    @Test
    public void testZonedDateTimeExampleValue() {
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody("", "", null);
        final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
        final ZonedDateTime example = ZonedDateTime.of(LocalDateTime.of(2016, 10, 16, 02, 12, 45), ZoneId.of("America/Los_Angeles"));
        object
            .timestamp("timestamp", "yyyy-MM-dd'T'HH:mm:ssZ", example)
            .time("time", "HH:mm:ssZ", example)
            .date("date", "yyyy-MM-dd", example);
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is("{\"date\":\"2016-10-16\",\"time\":\"02:12:45-0700\",\"timestamp\":\"2016-10-16T02:12:45-0700\"}"));
    }


    @Test
    public void testAndMatchingRules() {
        /*
            {
                "foo" : "Foo"
            }
         */

        // Old Dsl
        final String pactDslJson = new PactDslJsonBody()
                .and("foo", "Foo", PM.stringType(), PM.includesStr("F"), PM.includesStr("oo"))
                .getBody().toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
        object
                .and("foo", "Foo", PM.stringType(), PM.includesStr("F"), PM.includesStr("oo"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(3));
        assertThat(actualJson, containsString("foo"));
        assertThat(actualJson, containsString("Foo"));
        Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("type"));
        matcher = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("include"));
        matcher = actualPactDsl.getMatchers().allMatchingRules().get(2).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("include"));
    }

    @Test
    public void testOrMatchingRules() {
        /*
            {
                "foo" : null
            }
         */

        // Old Dsl
        final String pactDslJson = new PactDslJsonBody()
                .or("foo", null, PM.nullValue(), PM.booleanType(), PM.numberType())
                .getBody().toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
        object
                .or("foo", null, PM.nullValue(), PM.booleanType(), PM.numberType());
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(3));
        assertThat(actualJson, containsString("foo"));
        assertThat(actualJson, containsString("null"));
        Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("null"));
        matcher = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("type"));
        matcher = actualPactDsl.getMatchers().allMatchingRules().get(2).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("number"));
    }

    @Test
    public void testArray() {
        /*
            {
                "foo": [
                    {
                        "bar": "Bar"
                    }
                ]
            }
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .array("foo")
                .object()
                .stringValue("bar", "Bar")
                .closeObject()
                .closeArray()
                .getBody().toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
        object
                .array("foo", (array) -> array.object((o) -> o.stringValue("bar", "Bar")));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().isEmpty(), is(true));
    }

    @Test
    public void testObject() {
        /*
            {
                "foo": {
                    "bar": "Bar"
                }
            }
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .object("foo")
                .stringType("bar", "Bar")
                .closeObject()
                .getBody().toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
        object
                .object("foo", (o) -> o.stringValue("bar", "Bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().isEmpty(), is(true));

    }

    @Test
    public void testEachArrayLike() {
        /*
            {
                "foo": [
                    ["Bar"]
                ]
            }

         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .eachArrayLike("foo")
                .stringType("Bar")
                .closeArray()
                .closeArray()
                .getBody()
                .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslJsonBody object = new LambdaDslJsonBody(actualPactDsl);
        object
                .eachArrayLike("foo", a -> a.stringType("Bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("match"), is("type"));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(subArrayRule.get("match"), is("type"));
    }

    @Test
    public void testEachArrayLikeWithExample() {
        /*
            {
                "foo": [
                    ["Bar", "Bar"]
                ]
            }

         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .eachArrayLike("foo", 2)
                .stringType("Bar")
                .closeArray()
                .closeArray()
                .getBody()
                .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslJsonBody object = new LambdaDslJsonBody(actualPactDsl);
        object
                .eachArrayLike("foo", 2, a -> a.stringType("Bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("match"), is("type"));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(subArrayRule.get("match"), is("type"));
    }

    @Test
    public void testEachArrayWithMinLike() {
        /*
            {
                "foo": [
                    ["Bar"]
                ]
            }

         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .eachArrayWithMinLike("foo", 2)
                .stringType("Bar")
                .closeArray()
                .closeArray()
                .getBody()
                .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslJsonBody object = new LambdaDslJsonBody(actualPactDsl);
        object
                .eachArrayWithMinLike("foo", 2, a -> a.stringType("Bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("min"), is(2));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(subArrayRule.get("match"), is("type"));
    }

    @Test
    public void testEachArrayWithMinLikeWithExample() {
        /*
            {
                "foo": [
                    ["Bar"]
                ]
            }

         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .eachArrayWithMinLike("foo", 3, 2)
                .stringType("Bar")
                .closeArray()
                .closeArray()
                .getBody()
                .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslJsonBody object = new LambdaDslJsonBody(actualPactDsl);
        object
                .eachArrayWithMinLike("foo", 3, 2, a -> a.stringType("Bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("min"), is(2));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(subArrayRule.get("match"), is("type"));
    }

    @Test
    public void testEachArrayWithMaxLike() {
        /*
            {
                "foo": [
                    ["Bar"]
                ]
            }

         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .eachArrayWithMaxLike("foo", 2)
                .stringType("Bar")
                .closeArray()
                .closeArray()
                .getBody()
                .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslJsonBody object = new LambdaDslJsonBody(actualPactDsl);
        object
                .eachArrayWithMaxLike("foo", 2, a -> a.stringType("Bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("max"), is(2));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(subArrayRule.get("match"), is("type"));
    }

    @Test
    public void testEachArrayWithMaxLikeWithExample() {
        /*
            {
                "foo": [
                    ["Bar"]
                ]
            }

         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .eachArrayWithMaxLike("foo", 2, 3)
                .stringType("Bar")
                .closeArray()
                .closeArray()
                .getBody()
                .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslJsonBody object = new LambdaDslJsonBody(actualPactDsl);
        object
                .eachArrayWithMaxLike("foo", 2, 3, a -> a.stringType("Bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("max"), is(3));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(subArrayRule.get("match"), is("type"));
    }

    @Test
    public void testEachArrayWithMinMaxLike() {
        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
          .eachArrayWithMinMaxLike("foo", 2, 10)
          .stringType("Bar")
          .closeArray()
          .closeArray()
          .getBody()
          .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslJsonBody object = new LambdaDslJsonBody(actualPactDsl);
        object
          .eachArrayWithMinMaxLike("foo", 2, 10, a -> a.stringType("Bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("min"), is(2));
        assertThat(arrayRule.get("max"), is(10));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(subArrayRule.get("match"), is("type"));
    }

    @Test
    public void testEachArrayWithMinMaxLikeWithExample() {
        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
          .eachArrayWithMinMaxLike("foo", 2, 2, 10)
          .stringType("Bar")
          .closeArray()
          .closeArray()
          .getBody()
          .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslJsonBody object = new LambdaDslJsonBody(actualPactDsl);
        object
          .eachArrayWithMinMaxLike("foo", 2, 10, 2, a -> a.stringType("Bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("min"), is(2));
        assertThat(arrayRule.get("max"), is(10));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(subArrayRule.get("match"), is("type"));
    }

    @Test
    public void testEachLike() {
        /*
            {
                "foo": [
                    {
                        "bar": "string"
                    }
                ]
            }

         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .eachLike("foo")
                .stringType("bar")
                .closeArray()
                .getBody()
                .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslJsonBody object = new LambdaDslJsonBody(actualPactDsl);
        object
                .eachLike("foo", o -> o.stringType("bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("match"), is("type"));
        final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(arrayObjectRule.get("match"), is("type"));

    }

    @Test
    public void testEachLikeWithExample() {
        /*
            {
                "foo": [
                    {
                        "bar": "Bar"
                    },
                    {
                        "bar": "Bar"
                    }
                ]
            }

         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .eachLike("foo", 2)
                .stringType("bar", "Bar")
                .closeArray()
                .getBody()
                .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslJsonBody object = new LambdaDslJsonBody(actualPactDsl);
        object
                .eachLike("foo", 2, o -> o.stringType("bar", "Bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("match"), is("type"));
        final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(arrayObjectRule.get("match"), is("type"));

    }

    @Test
    public void testMinArrayLike() {
        /*
            {
                "foo": [
                    {
                        "bar": "string"
                    },
                    {
                        "bar": "string"
                    }
                ]
            }
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .minArrayLike("foo", 2)
                .stringType("bar")
                .closeObject()
                .closeArray()
                .getBody()
                .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslObject object = new LambdaDslJsonBody(actualPactDsl);
        object
                .minArrayLike("foo", 2, o -> o.stringType("bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("min"), is(2));
        final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(arrayObjectRule.get("match"), is("type"));
    }

    @Test
    public void testMinArrayLikeWithExample() {
        /*
            {
                "foo": [
                    {
                        "bar": "Bar"
                    },
                    {
                        "bar": "Bar"
                    },
                    {
                        "bar": "Bar"
                    }
                ]
            }
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .minArrayLike("foo", 2, 3)
                .stringType("bar", "Bar")
                .closeObject()
                .closeArray()
                .getBody()
                .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslObject object = new LambdaDslJsonBody(actualPactDsl);
        object
                .minArrayLike("foo", 2, 3, o -> o.stringType("bar", "Bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> rule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(rule.get("min"), is(2));
        final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(arrayObjectRule.get("match"), is("type"));
    }

    @Test
    public void testMaxArrayLike() {
        /*
            {
                "foo": [
                    {
                        "bar": "string"
                    }
                ]
            }
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .maxArrayLike("foo", 2)
                .stringType("bar")
                .closeObject()
                .closeArray()
                .getBody()
                .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslObject object = new LambdaDslJsonBody(actualPactDsl);
        object
                .maxArrayLike("foo", 2, o -> o.stringType("bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> rule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(rule.get("max"), is(2));
        final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(arrayObjectRule.get("match"), is("type"));
    }

    @Test
    public void testMaxArrayLikeWithExample() {
        /*
            {
                "foo": [
                    {
                        "bar": "Bar"
                    },
                    {
                        "bar": "Bar"
                    }
                ]
            }
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .maxArrayLike("foo", 3, 2)
                .stringType("bar", "Bar")
                .closeObject()
                .closeArray()
                .getBody()
                .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslObject object = new LambdaDslJsonBody(actualPactDsl);
        object
                .maxArrayLike("foo", 3, 2, o -> o.stringType("bar", "Bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> rule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(rule.get("max"), is(3));
        final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(arrayObjectRule.get("match"), is("type"));
    }

  @Test
  public void testMinMaxArrayLike() {
    // Old DSL
    final String pactDslJson = new PactDslJsonBody()
      .minMaxArrayLike("foo", 2, 7)
      .stringType("bar")
      .closeObject()
      .closeArray()
      .getBody()
      .toString();

    // Lambda DSL
    final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
    final LambdaDslObject object = new LambdaDslJsonBody(actualPactDsl);
    object
      .minMaxArrayLike("foo", 2, 7, o -> o.stringType("bar"));
    actualPactDsl.close();

    String actualJson = actualPactDsl.getBody().toString();
    assertThat(actualJson, is(pactDslJson));
    assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
    final Map<String, Object> rule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
    assertThat(rule.get("min"), is(2));
    assertThat(rule.get("max"), is(7));
    final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
    assertThat(arrayObjectRule.get("match"), is("type"));
  }

  @Test
  public void testMinMaxArrayLikeWithExample() {
    // Old DSL
    final String pactDslJson = new PactDslJsonBody()
      .minMaxArrayLike("foo", 3, 5, 3)
      .stringType("bar", "Bar")
      .closeObject()
      .closeArray()
      .getBody()
      .toString();

    // Lambda DSL
    final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
    final LambdaDslObject object = new LambdaDslJsonBody(actualPactDsl);
    object
      .minMaxArrayLike("foo", 3, 5, 3, o -> o.stringType("bar", "Bar"));
    actualPactDsl.close();

    String actualJson = actualPactDsl.getBody().toString();
    assertThat(actualJson, is(pactDslJson));
    assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
    final Map<String, Object> rule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
    assertThat(rule.get("min"), is(3));
    assertThat(rule.get("max"), is(5));
    final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
    assertThat(arrayObjectRule.get("match"), is("type"));
  }

    @Test
    public void testEachKeyLike() {
        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .object("one")
                .eachKeyLike("001-A") // key like an id where the value is matched by the following example
                .stringType("description", "Some Description")
                .closeObject()
                .closeObject()
                .getBody()
                .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslObject object = new LambdaDslJsonBody(actualPactDsl);
        object.object("one",
                      o -> o.eachKeyLike("001-A",
                                         nested -> nested.stringType("description", "Some Description")));

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
    }

    @Test
    public void testEachKeyMappedToAnArrayLike() {
        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
                .object("one")
                .eachKeyMappedToAnArrayLike("001")
                .id("someId", 23456L)
                .closeObject()
                .closeArray()
                .closeObject()
                .getBody()
                .toString();

        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslObject object = new LambdaDslJsonBody(actualPactDsl);
        object.object("one",
                      o -> o.eachKeyMappedToAnArrayLike("001",
                                                        nested -> nested.id("someId", 23456L)));

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
    }

  @Test
  public void testStringArray() {
    /*
        {
            "foo": [ "one", "one", "one" ]
        }
     */

    // Old DSL
    final String pactDslJson = new PactDslJsonBody()
      .minArrayLike("foo", 3, PactDslJsonRootValue.stringType("one"), 3)
      .maxArrayLike("foo2", 3, PactDslJsonRootValue.stringType("one"), 3)
      .minMaxArrayLike("foo3", 3, 10, PactDslJsonRootValue.stringType("one"), 3)
      .getBody().toString();

    // Lambda DSL
    final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
    final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
    object
      .minArrayLike("foo", 3, PactDslJsonRootValue.stringType("one"), 3)
      .maxArrayLike("foo2", 3, PactDslJsonRootValue.stringType("one"), 3)
      .minMaxArrayLike("foo3", 3, 10, PactDslJsonRootValue.stringType("one"), 3);
    actualPactDsl.close();

    String actualJson = actualPactDsl.getBody().toString();
    assertThat(actualJson, is(pactDslJson));
    assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(6));
    Map<String, Object> rule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
    assertThat(rule.get("min"), is(3));
    Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
    assertThat(arrayObjectRule.get("match"), is("type"));
    rule = actualPactDsl.getMatchers().allMatchingRules().get(2).toMap(PactSpecVersion.V3);
    assertThat(rule.get("max"), is(3));
    arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(3).toMap(PactSpecVersion.V3);
    assertThat(arrayObjectRule.get("match"), is("type"));
    rule = actualPactDsl.getMatchers().allMatchingRules().get(4).toMap(PactSpecVersion.V3);
    assertThat(rule.get("min"), is(3));
    assertThat(rule.get("max"), is(10));
    arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(5).toMap(PactSpecVersion.V3);
    assertThat(arrayObjectRule.get("match"), is("type"));
  }

    @Test
    public void testUnorderedArrayMatcher() {
        // Old DSL
        final DslPart pactDslJson = new PactDslJsonBody()
            .unorderedArray("foo")
            .stringValue("a")
            .stringValue("b")
            .stringValue("c")
            .closeArray()
            .close();

        // Lambda DSL
        final DslPart lambdaPactDsl = LambdaDsl.newJsonBody(body ->
            body.unorderedArray("foo", foo ->
                foo.stringValue("a")
                    .stringValue("b")
                    .stringValue("c")
            )
        ).build().close();

        assertThat(lambdaPactDsl.getBody().toString(), is(pactDslJson.getBody().toString()));
        assertThat(lambdaPactDsl.getMatchers(), is(pactDslJson.getMatchers()));
    }

}
