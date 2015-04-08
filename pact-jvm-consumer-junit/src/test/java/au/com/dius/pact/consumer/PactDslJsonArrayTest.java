package au.com.dius.pact.consumer;

import au.com.dius.pact.model.PactFragment;

public class PactDslJsonArrayTest extends ConsumerPactTest {
	@Override
	protected PactFragment createFragment(ConsumerPactBuilder.PactDslWithProvider builder) {
		DslPart body = new PactDslJsonArray()
				.object()
					.id()
					.stringValue("name", "Rogger the Dogger")
					.timestamp()
					.date("dob", "MM/dd/yyyy")
				.closeObject()
				.object()
					.id()
					.stringValue("name", "Cat in the Hat")
					.timestamp()
					.date("dob", "MM/dd/yyyy")
				.closeObject();
		PactFragment fragment = builder
				.uponReceiving("java test interaction with a DSL array body")
				.path("/")
				.method("GET")
				.willRespondWith()
				.status(200)
				.body(body)
				.toFragment();

		MatcherTestUtils.assertResponseMatchersEqualTo(fragment,
				"$.body[0].id",
				"$.body[0].timestamp",
				"$.body[0].dob",
				"$.body[1].id",
				"$.body[1].timestamp",
				"$.body[1].dob"
				);

		return fragment;
	}

	@Override
	protected String providerName() {
		return "test_provider";
	}

	@Override
	protected String consumerName() {
		return "test_consumer";
	}

	@Override
	protected void runTest(String url) {
		try {
			new ConsumerClient(url).getAsList("/");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}