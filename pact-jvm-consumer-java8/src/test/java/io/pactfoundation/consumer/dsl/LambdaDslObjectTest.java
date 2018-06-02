package io.pactfoundation.consumer.dsl;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.Test;

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
    public void testStringMatcher() {
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
        final Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
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
        final Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
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
        final Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
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
        Map matcher = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(matcher.get("match"), is("type"));
        matcher = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
        assertThat(matcher.get("match"), is("type"));
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
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(arrayRule.get("match"), is("type"));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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

        System.out.println(pactDslJson);
        // Lambda DSL
        final PactDslJsonBody actualPactDsl = new PactDslJsonBody();
        final LambdaDslJsonBody object = new LambdaDslJsonBody(actualPactDsl);
        object
                .eachArrayLike("foo", 2, a -> a.stringType("Bar"));
        actualPactDsl.close();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
        assertThat(actualPactDsl.getMatchers().allMatchingRules().size(), is(2));
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(arrayRule.get("match"), is("type"));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(arrayRule.get("min"), is(2));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(arrayRule.get("min"), is(2));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(arrayRule.get("max"), is(2));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(arrayRule.get("max"), is(3));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(arrayRule.get("min"), is(2));
        assertThat(arrayRule.get("max"), is(10));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(arrayRule.get("min"), is(2));
        assertThat(arrayRule.get("max"), is(10));
        final Map<String, Object> subArrayRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(arrayRule.get("match"), is("type"));
        final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(arrayRule.get("match"), is("type"));
        final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
        final Map<String, Object> arrayRule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(arrayRule.get("min"), is(2));
        final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
        final Map<String, Object> rule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(rule.get("min"), is(2));
        final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
        final Map<String, Object> rule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(rule.get("max"), is(2));
        final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
        final Map<String, Object> rule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
        assertThat(rule.get("max"), is(3));
        final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
    final Map<String, Object> rule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
    assertThat(rule.get("min"), is(2));
    assertThat(rule.get("max"), is(7));
    final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
    final Map<String, Object> rule = actualPactDsl.getMatchers().allMatchingRules().get(0).toMap();
    assertThat(rule.get("min"), is(3));
    assertThat(rule.get("max"), is(5));
    final Map<String, Object> arrayObjectRule = actualPactDsl.getMatchers().allMatchingRules().get(1).toMap();
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
}
