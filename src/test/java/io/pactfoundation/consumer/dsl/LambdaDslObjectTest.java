package io.pactfoundation.consumer.dsl;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LambdaDslObjectTest {

    @Test
    public void testStringValue() throws IOException {
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
    public void testStringMatcher() throws IOException {
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody("", "", null);
        final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
        object
            .stringMatcher("foo", "[a-z][0-9]");
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString().replace("\"", "'");
        assertThat(actualJson, containsString("{'foo':"));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(1));
        final Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(matcher.get("match"), is("regex"));
        assertThat(matcher.get("regex"), is("[a-z][0-9]"));
    }

    @Test
    public void testStringMatcherWithExample() throws IOException {
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
        final Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(matcher.get("match"), is("regex"));
        assertThat(matcher.get("regex"), is("[a-z][0-9]"));
    }

    @Test
    public void testStringType() throws IOException {
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslObject object = new LambdaDslObject(actualPactDsl);
        object
            .stringType("foo");
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString().replace("\"", "'");
        assertThat(actualJson, containsString("{'foo':"));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(1));
        final Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(matcher.get("match"), is("type"));
    }

    @Test
    public void testStringTypeWithExample() throws IOException {
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
        final Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(matcher.get("match"), is("type"));
        System.out.println(actualJson);
    }

    @Test
    public void testStringTypes() throws IOException {
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
        Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(matcher.get("match"), is("type"));
        matcher = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
        assertThat(matcher.get("match"), is("type"));
        System.out.println(actualJson);
    }

    @Test
    public void testArray() throws IOException {
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
            .array("foo", (array) -> {
                array.object((o) -> {
                    o.stringValue("bar", "Bar");
                });
            });
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().isEmpty(), is(true));
    }

    @Test
    public void testObject() throws IOException {
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
            .object("foo", (o) -> {
                o.stringValue("bar", "Bar");
            });
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().isEmpty(), is(true));

    }
}
