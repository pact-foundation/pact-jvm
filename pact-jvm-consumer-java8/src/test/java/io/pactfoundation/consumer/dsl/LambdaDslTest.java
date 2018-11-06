package io.pactfoundation.consumer.dsl;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LambdaDslTest {

    @Test
    public void testArrayWithObjects() throws IOException {
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
    public void testObjectWithObjects() throws IOException {
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
    public void testObjectWithComplexStructure() throws IOException {
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
}
