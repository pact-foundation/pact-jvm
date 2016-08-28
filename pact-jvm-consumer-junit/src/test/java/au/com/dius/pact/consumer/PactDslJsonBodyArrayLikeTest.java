package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.model.PactFragment;

public class PactDslJsonBodyArrayLikeTest extends ConsumerPactTest {

    @Override
    protected PactFragment createFragment(PactDslWithProvider builder) {
        DslPart body = new PactDslJsonBody()
            .id()
            .object("data")
                .eachLike("array1")
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
                .closeArray()
            .closeObject();
        PactFragment fragment = builder
                .uponReceiving("java test interaction with an array like matcher")
                .path("/")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(body)
                .toFragment();

        MatcherTestUtils.assertResponseMatcherKeysEqualTo(fragment, "body",
            "$.id",
            "$.data.array1",
            "$.data.array1[*].id",
            "$.data.array1[*].name",
            "$.data.array1[*].dob",
            "$.data.array2",
            "$.data.array2[*].address",
            "$.data.array2[*].name",
            "$.data.array3[0]",
            "$.data.array3[0][*].itemCount");

        MatcherTestUtils.assertResponseKeysEqualTo(fragment,
            "/data",
            "/data/array1",
            "/data/array1/0/dob",
            "/data/array1/0/id",
            "/data/array1/0/name",
            "/data/array2",
            "/data/array2/0/address",
            "/data/array2/0/name",
            "/data/array3/0/0/itemCount",
            "/data/array3",
            "/id");

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
            new ConsumerClient(url).getAsMap("/", "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
