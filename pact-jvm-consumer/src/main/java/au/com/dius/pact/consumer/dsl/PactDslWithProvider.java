package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.ConsumerPactBuilder;

public class PactDslWithProvider {
    private ConsumerPactBuilder consumerPactBuilder;
    private String providerName;

    public PactDslWithProvider(ConsumerPactBuilder consumerPactBuilder, String provider) {
        this.consumerPactBuilder = consumerPactBuilder;
        this.providerName = provider;
    }

    /**
     * Describe the state the provider needs to be in for the pact test to be verified.
     *
     * @param state Provider state
     */
    public PactDslWithState given(String state) {
        return new PactDslWithState(consumerPactBuilder, consumerPactBuilder.getConsumerName(), providerName, state);
    }

    /**
     * Description of the request that is expected to be received
     *
     * @param description request description
     */
    public PactDslRequestWithoutPath uponReceiving(String description) {
        return new PactDslWithState(consumerPactBuilder, consumerPactBuilder.getConsumerName(), providerName, null)
                .uponReceiving(description);
    }

}
