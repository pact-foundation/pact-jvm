package au.com.dius.pact.consumer;

import au.com.dius.pact.model.PactFragment;

public class PactDslJsonBodyArrayLikeTest extends ConsumerPactTest {

    @Override
    protected PactFragment createFragment(ConsumerPactBuilder.PactDslWithProvider builder) {
        DslPart body = new PactDslJsonBody()
            .id()
            .arrayLike("array1")
                .id()
                .stringType("name")
                .date("dob")
                .closeObject()
            .closeArray()
            .minArrayLike("array2", 1)
                .ipAddress("address")
                .stringType("name")
                .closeObject()
            .closeArray()
            .array("array3")
                .maxArrayLike(5)
                    .integerType("itemCount")
                    .closeObject()
                .closeArray()
            .closeArray();
        PactFragment fragment = builder
                .uponReceiving("java test interaction with an array like matcher")
                .path("/")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(body)
                .toFragment();

        MatcherTestUtils.assertResponseMatcherKeysEqualTo(fragment,
            "$.body.id",
            "$.body.array1[*].id",
            "$.body.array1[*].name",
            "$.body.array1[*].dob",
            "$.body.array2",
            "$.body.array2[*].address",
            "$.body.array2[*].name",
            "$.body.array3[0]",
            "$.body.array3[0][*].itemCount");

        return fragment;
    }

    @Override
    protected String providerName() {
        return "test_provider_array";
    }

    @Override
    protected String consumerName() {
        return "test_consumer_array";
    }

    @Override
    protected void runTest(String url) {
        try {
            new ConsumerClient(url).getAsMap("/");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
