package au.com.dius.pact.consumer;

import au.com.dius.pact.model.PactFragment;

public class PactDslJsonArrayTemplateTest extends ConsumerPactTest {
	@Override
	protected PactFragment createFragment(ConsumerPactBuilder.PactDslWithProvider builder) {
		DslPart personTemplate = new PactDslJsonBody()
				.id()
				.stringType("name")
				.date("dob");
		
		DslPart body = new PactDslJsonArray()
				.template(personTemplate, 3);
				
		return builder
				.uponReceiving("java test interaction with a DSL array body with templates")
					.path("/")
					.method("GET")
				.willRespondWith()
					.status(200)
					.body(body)
				.toFragment();
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
