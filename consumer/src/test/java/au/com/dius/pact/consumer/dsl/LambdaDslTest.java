package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory;
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LambdaDslTest {

    @Test
    public void testArrayWithObjects() {
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
        final String pactDslJson = new PactDslJsonArray()
            .object()
            .stringValue("foo", "Foo")
            .closeObject()
            .object()
            .stringValue("bar", "Bar")
            .closeObject()
            .getBody().toString();

        // Lambda DSL
        final DslPart actualPactDsl = LambdaDsl.newJsonArray((array) -> {
            array
                .object((o) -> {
                    o.stringValue("foo", "Foo");
                })
                .object((o) -> {
                    o.stringValue("bar", "Bar");
                });
        })
            .build();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));

    }

    @Test
    public void testObjectWithObjects() {
        /*
            {
                "propObj1": {
                    "foo": "Foo"
                },
                "propObj2": {
                    "bar": "Bar"
                },
                "someProperty": "Prop"
            }
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
            .stringValue("someProperty", "Prop")
            .object("propObj1")
            .stringValue("foo", "Foo")
            .closeObject()
            .object("propObj2")
            .stringValue("bar", "Bar")
            .closeObject()
            .getBody().toString();

        // Lambda DSL
        final DslPart actualPactDsl = LambdaDsl.newJsonBody((body) -> {
            body
                .stringValue("someProperty", "Prop")
                .object("propObj1", (o) -> {
                    o.stringValue("foo", "Foo");
                })
                .object("propObj2", (o) -> {
                    o.stringValue("bar", "Bar");
                });
        })
            .build();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
    }

    @Test
    public void testObjectWithComplexStructure() {
        /*
            {
                "propObj1": {
                    "foo": "Foo",
                    "someProperty": 1
                },
                "someArray": [
                    {
                        "arrayObj1Prop1": "ao1p1"
                    },
                    {
                        "arrayObj1Prop2Obj": {
                            "arrayObj1Prop2ObjProp1": "ao1p2op1"
                        },
                        "arrayObj2Prop1": "ao2p1"
                    }
                ]
            }
         */

        // Old DSL
        final String pactDslJson = new PactDslJsonBody()
            .object("propObj1")
            .stringValue("foo", "Foo")
            .numberValue("someProperty", 1L)
            .closeObject()
            .array("someArray")
            .object()
            .stringValue("arrayObj1Prop1", "ao1p1")
            .closeObject()
            .object()
            .stringValue("arrayObj2Prop1", "ao2p1")
            .object("arrayObj1Prop2Obj")
            .stringValue("arrayObj1Prop2ObjProp1", "ao1p2op1")
            .closeObject()
            .closeObject()
            .closeArray()
            .getBody().toString();

        // Lambda DSL
        final DslPart actualPactDsl = LambdaDsl.newJsonBody((body) -> {
            body
                .object("propObj1", (o) -> {
                    o.stringValue("foo", "Foo");
                    o.numberValue("someProperty", 1L);
                })
                .array("someArray", (a) -> {
                    a.object((oo) -> oo.stringValue("arrayObj1Prop1", "ao1p1"));
                    a.object((oo) -> {
                        oo.stringValue("arrayObj2Prop1", "ao2p1");
                        oo.object("arrayObj1Prop2Obj", (ooo) -> ooo.stringValue("arrayObj1Prop2ObjProp1", "ao1p2op1"));
                    });
                });
        })
            .build();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
    }

    @Test
    public void testArrayMinLike() {
        /*
            [
                {
                    "foo": "string"
                },
                {
                    "foo": "string"
                }
            ]
         */

        String pactDslJson = PactDslJsonArray.arrayMinLike(2)
            .stringType("foo")
            .close()
            .getBody()
            .toString();

        DslPart actualPactDsl = LambdaDsl.newJsonArrayMinLike(2, o -> o.object(
            oo -> oo.stringType("foo")
        )).build();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
    }

    @Test
    public void testArrayMaxLike() {
        /*
            [
                {
                    "foo": "string"
                }
            ]
         */

        String pactDslJson = PactDslJsonArray.arrayMaxLike(2)
            .stringType("foo")
            .close()
            .getBody()
            .toString();

        DslPart actualPactDsl = LambdaDsl.newJsonArrayMaxLike(2, o -> o.object(
            oo -> oo.stringType("foo")
        )).build();

        String actualJson = actualPactDsl.getBody().toString();
        assertThat(actualJson, is(pactDslJson));
    }

    @Test
    public void testNumberValue() {
        DslPart dslPart = LambdaDsl.newJsonBody(o -> o.numberValue("number", 1)).build();
        assertThat(dslPart.getBody().toString(), is("{\"number\":1}"));
    }

    @Test
    public void testUnorderedArrayWithObjects() {
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
        final DslPart pactDslJson = PactDslJsonArray
            .newUnorderedArray()
            .object()
            .stringValue("foo", "Foo")
            .closeObject()
            .object()
            .stringValue("bar", "Bar")
            .closeObject()
            .close();

        // Lambda DSL
        final DslPart lambdaPactDsl = LambdaDsl.newJsonArrayUnordered(array ->
            array
                .object(o ->
                    o.stringValue("foo", "Foo")
                )
                .object(o ->
                    o.stringValue("bar", "Bar")
                )
        ).build().close();

        assertThat(lambdaPactDsl.getBody().toString(), is(pactDslJson.getBody().toString()));
        assertThat(lambdaPactDsl.getMatchers(), is(pactDslJson.getMatchers()));
    }

    @Test
    public void attribute_that_is_a_url() {
        DslPart jsonBody = LambdaDsl.newJsonBody((body) -> {
            body.nullValue("error");
            body.stringValue("iss", "f2f");
            body.stringValue("sub", "test-subject");
            body.stringType("state", "f5f0d4d1-b937-4abe-b379-8269f600ad44");
            body.minArrayLike(
                "https://vocab.account.gov.uk/v1/credentialJWT",
                1,
                PactDslJsonRootValue.stringMatcher("[a-fA-F0-9]+", "0123456789abcdef"), 1);
            body.nullValue("error_description");
        }).build().close();

        assertThat(jsonBody.getBody().toString(), is("{\"error\":null,\"error_description\":null,\"https://vocab.account.gov.uk/v1/credentialJWT\":[\"0123456789abcdef\"],\"iss\":\"f2f\",\"state\":\"f5f0d4d1-b937-4abe-b379-8269f600ad44\",\"sub\":\"test-subject\"}"));
        assertThat(jsonBody.getMatchers(), is(equalTo(new MatchingRuleCategory("body", Map.of(
            "$.state", new MatchingRuleGroup(List.of(au.com.dius.pact.core.model.matchingrules.TypeMatcher.INSTANCE)),
            "$['https://vocab.account.gov.uk/v1/credentialJWT']", new MatchingRuleGroup(List.of(new au.com.dius.pact.core.model.matchingrules.MinTypeMatcher(1))),
            "$['https://vocab.account.gov.uk/v1/credentialJWT'][*]", new MatchingRuleGroup(List.of(new au.com.dius.pact.core.model.matchingrules.RegexMatcher("[a-fA-F0-9]+", "0123456789abcdef")))
        )))));
    }
}
