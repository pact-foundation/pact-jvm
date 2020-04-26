package io.pactfoundation.consumer.dsl;

import au.com.dius.pact.consumer.dsl.PM;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.core.model.PactSpecVersion;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LambdaDslJsonArrayTest {

    @Test
    public void testObjectArray() {
        /*
            [
                {
                    "foo": "Foo"
                },
                {
                    "bar": "Bar"
                }
            ]
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonArray("", "", null, false)
                .object()
                .stringValue("foo", "Foo")
                .closeObject()
                .object()
                .stringType("bar", "Bar")
                .closeObject()
                .getBody().toString();

        // Lambda DSL
        final PactDslJsonArray actualPactDsl = new PactDslJsonArray("", "", null, false);
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .object((o) -> o.stringValue("foo", "Foo"))
                .object((o) -> o.stringValue("bar", "Bar"))
                .build();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().isEmpty(), is(true));

    }

    @Test
    public void testStringArray() {
        /*
            [
                "Foo",
                "Bar",
                "x"
            ]
         */
        // Old DSL
        final String pactDslJson = new PactDslJsonArray("", "", null, false)
                .string("Foo")
                .stringType("Bar")
                .stringMatcher("[a-z]", "x")
                .getBody().toString();

        // Lambda DSL
        final PactDslJsonArray actualPactDsl = new PactDslJsonArray("", "", null, false);
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .stringValue("Foo")
                .stringType("Bar")
                .stringMatcher("[a-z]", "x")
                .build();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
    }

    @Test
    public void testNumberArray() {
        /*
            [
                1,
                2,
                3
            ]
         */
        // Old DSL
        final String pactDslJson = new PactDslJsonArray("", "", null, false)
                .numberValue(1)
                .numberValue(2)
                .numberValue(3)
                .getBody().toString();

        // Lambda DSL
        final PactDslJsonArray actualPactDsl = new PactDslJsonArray("", "", null, false);
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .numberValue(1)
                .numberValue(2)
                .numberValue(3)
                .build();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
    }

    @Test
    public void testAndMatchingRules() {
        /*
            [
                "fooBar"
            ]
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonArray("", "", null, false)
                .and("foobar", PM.stringType(), PM.includesStr("foo"), PM.stringMatcher("*Bar"))
                .getBody().toString();

        // Lambda DSL
        final PactDslJsonArray actualPactDsl = new PactDslJsonArray("", "", null, false);
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .and("foobar", PM.stringType(), PM.includesStr("foo"), PM.stringMatcher("*Bar"))
                .build();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(3));
        Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("type"));
        matcher = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("include"));
        matcher = actualPactDsl.getMatchers().allMatchingRules().get(2).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("regex"));
    }

    @Test
    public void testOrMatchingRules() {
        /*
            [
                null
            ]
         */
        // Old DSL
        final String pactDslJson = new PactDslJsonArray("", "", null, false)
                .or(null, PM.nullValue(), PM.date(), PM.ipAddress())
                .getBody().toString();

        // Lambda DSL
        final PactDslJsonArray actualPactDsl = new PactDslJsonArray("", "", null, false);
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .or(null, PM.nullValue(), PM.date(), PM.ipAddress())
                .build();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(3));
        Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("null"));
        matcher = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("date"));
        matcher = actualPactDsl.getMatchers().allMatchingRules().get(2).toMap(PactSpecVersion.V3);
        assertThat(matcher.get("match"), is("regex"));
    }

    @Test
    public void testArrayArray() {
        /*
            [
                ["a1", "a2"],
                [1, 2],
                [{"foo": "Foo"}]
            ]
         */
        // Old DSL
        final String pactDslJson = new PactDslJsonArray("", "", null, false)
                .array()
                .stringValue("a1")
                .stringValue("a2")
                .closeArray()
                .array()
                .numberValue(1)
                .numberValue(2)
                .closeArray()
                .array()
                .object()
                .stringValue("foo", "Foo")
                .closeObject()
                .closeArray()
                .getBody().toString();

        // Lambda DSL
        final PactDslJsonArray actualPactDsl = new PactDslJsonArray("", "", null, false);
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .array((a) -> a.stringValue("a1").stringValue("a2"))
                .array((a) -> a.numberValue(1).numberValue(2))
                .array((a) -> a.object((o) -> o.stringValue("foo", "Foo")))
                .build();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
    }

    @Test
    public void testEachArrayLike() {
        /*
            [
                [
                    ["Foo"]
                ]
            ]
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
                .eachArrayLike()
                .stringType("Foo")
                .closeArray()
                .closeArray()
                .getBody()
                .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .eachArrayLike(a -> a.stringType("Foo"))
                .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("match"), is("type"));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testEachArrayLikeWithExample() {
        /*
            [
                [
                    ["Foo", "Foo"]
                ]
            ]
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
                .eachArrayLike(2)
                .stringType("Foo")
                .closeArray()
                .closeArray()
                .getBody()
                .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .eachArrayLike(2, a -> a.stringType("Foo"))
                .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("match"), is("type"));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testEachArrayWithMinLike() {
        /*
            [
                [
                    ["Foo"]
                ]
            ]
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
                .eachArrayWithMinLike(2)
                .stringType("Foo")
                .closeArray()
                .closeArray()
                .getBody()
                .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .eachArrayWithMinLike(2, a -> a.stringType("Foo"))
                .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("min"), is(2));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testEachArrayWithMinLikeWithExample() {
        /*
            [
                [
                    ["Foo", "Foo", "Foo"]
                ]
            ]
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
                .eachArrayWithMinLike(3, 2)
                .stringType("Foo")
                .closeArray()
                .closeArray()
                .getBody()
                .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .eachArrayWithMinLike(3, 2, a -> a.stringType("Foo"))
                .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("min"), is(2));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testEachArrayWithMaxLike() {
        /*
            [
                [
                    ["Foo"]
                ]
            ]
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
                .eachArrayWithMaxLike(2)
                .stringType("Foo")
                .closeArray()
                .closeArray()
                .getBody()
                .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .eachArrayWithMaxLike(2, a -> a.stringType("Foo"))
                .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("max"), is(2));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testEachArrayWithMaxLikeWithExample() {
        /*
            [
                [
                    ["Foo", "Foo"]
                ]
            ]
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
                .eachArrayWithMaxLike(2, 3)
                .stringType("Foo")
                .closeArray()
                .closeArray()
                .getBody()
                .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .eachArrayWithMaxLike(2, 3, a -> a.stringType("Foo"))
                .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("max"), is(3));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testEachArrayWithMinMaxLike() {
        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
          .eachArrayWithMinMaxLike(2, 10)
          .stringType("Foo")
          .closeArray()
          .closeArray()
          .getBody()
          .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
          .eachArrayWithMinMaxLike(2, 10, a -> a.stringType("Foo"))
          .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("min"), is(2));
        assertThat(arrayRule.get("max"), is(10));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testEachArrayWithMinMaxLikeWithExample() {
        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
          .eachArrayWithMinMaxLike(2, 2, 10)
          .stringType("Foo")
          .closeArray()
          .closeArray()
          .getBody()
          .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
          .eachArrayWithMinMaxLike(2, 10, 2, a -> a.stringType("Foo"))
          .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("min"), is(2));
        assertThat(arrayRule.get("max"), is(10));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testEachLike() {
        /*
            [
                [
                    {
                        "foo": "string"
                    }
                ]
            ]
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
                .eachLike()
                .stringType("foo")
                .closeArray()
                .getBody()
                .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .eachLike(o -> o.stringType("foo"))
                .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("match"), is("type"));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testEachLikeWithExample() {
        /*
            [
                [
                    {
                        "foo": "string"
                    },
                    {
                        "foo": "string"
                    }
                ]
            ]
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
                .eachLike(2)
                .stringType("foo")
                .closeArray()
                .getBody()
                .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .eachLike(2, o -> o.stringType("foo"))
                .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("match"), is("type"));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testMinArrayLike() {
        /*
            [
                [
                    {
                        "foo": "string"
                    },
                    {
                        "foo": "string"
                    }
                ]
            ]
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
                .minArrayLike(2)
                .stringType("foo")
                .closeArray()
                .getBody()
                .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .minArrayLike(2, o -> o.stringType("foo"))
                .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("min"), is(2));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testMinArrayLikeWithExample() {
        /*
            [
                [
                    {
                        "foo": "string"
                    },
                    {
                        "foo": "string"
                    },
                    {
                        "foo": "string"
                    }
                ]
            ]
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
                .minArrayLike(2, 3)
                .stringType("foo")
                .closeArray()
                .getBody()
                .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .minArrayLike(2, 3, o -> o.stringType("foo"))
                .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("min"), is(2));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testMaxArrayLike() {
        /*
            [
                [
                    {
                        "foo": "string"
                    }
                ]
            ]
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
                .maxArrayLike(2)
                .stringType("foo")
                .closeArray()
                .getBody()
                .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .maxArrayLike(2, o -> o.stringType("foo"))
                .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("max"), is(2));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testMaxArrayLikeWithExample() {
        /*
            [
                [
                    {
                        "foo": "string"
                    },
                    {
                        "foo": "string"
                    }
                ]
            ]
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
                .maxArrayLike(3, 2)
                .stringType("foo")
                .closeArray()
                .getBody()
                .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
                .maxArrayLike(3, 2, o -> o.stringType("foo"))
                .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("max"), is(3));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testMinMaxArrayLike() {
        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
          .minMaxArrayLike(2, 5)
          .stringType("foo")
          .closeArray()
          .getBody()
          .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
          .minMaxArrayLike(2, 5, o -> o.stringType("foo"))
          .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("min"), is(2));
        assertThat(arrayRule.get("max"), is(5));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }

    @Test
    public void testMinMaxArrayLikeWithExample() {
        // Old DSL
        final String pactDslJson = new PactDslJsonArray()
          .minMaxArrayLike(3, 8, 4)
          .stringType("foo")
          .closeArray()
          .getBody()
          .toString();

        final PactDslJsonArray actualPactDsl = new PactDslJsonArray();
        final LambdaDslJsonArray array = new LambdaDslJsonArray(actualPactDsl);
        array
          .minMaxArrayLike(3, 8, 4, o -> o.stringType("foo"))
          .build();

        final String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap(PactSpecVersion.V3);
        assertThat(arrayRule.get("min"), is(3));
        assertThat(arrayRule.get("max"), is(8));
        final Map<String, Object> objectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap(PactSpecVersion.V3);
        assertThat(objectRule.get("match"), is("type"));
    }
}
