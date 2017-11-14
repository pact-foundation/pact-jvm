package io.pactfoundation.consumer.dsl;

import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LambdaDslJsonArrayTest {

    @Test
    public void testObjectArray() throws IOException {
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
            .object((o) -> {
                o.stringValue("foo", "Foo");
            })
            .object((o) -> {
                o.stringValue("bar", "Bar");
            })
            .build();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().isEmpty(), is(true));

    }

    @Test
    public void testStringArray() throws IOException {
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
    public void testNumberArray() throws IOException {
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
    public void testArrayArray() throws IOException {
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

}
