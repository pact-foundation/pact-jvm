package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.ConsumerPactBuilder;

public class PactDslWithState {
    private final ConsumerPactBuilder consumerPactBuilder;
    String state;
    String consumerName;
    String providerName;

    public PactDslWithState(ConsumerPactBuilder consumerPactBuilder, String consumerName, String providerName, String state) {
        this.consumerPactBuilder = consumerPactBuilder;
        this.consumerName = consumerName;
        this.providerName = providerName;
        this.state = state;
    }

    /**
     * Description of the request that is expected to be received
     *
     * @param description request description
     */
    public PactDslRequestWithoutPath uponReceiving(String description) {
        return new PactDslRequestWithoutPath(consumerPactBuilder, this, description);
    }

}
