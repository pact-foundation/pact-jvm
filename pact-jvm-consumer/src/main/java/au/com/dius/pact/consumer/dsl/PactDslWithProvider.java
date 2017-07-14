package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.model.ProviderState;

import java.util.HashMap;
import java.util.Map;

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
        return new PactDslWithState(consumerPactBuilder, consumerPactBuilder.getConsumerName(), providerName,
          new ProviderState(state));
    }

    /**
     * Describe the state the provider needs to be in for the pact test to be verified.
     *
     * @param state Provider state
     * @param params Data parameters for the state
     */
    public PactDslWithState given(String state, Map<String, Object> params) {
        return new PactDslWithState(consumerPactBuilder, consumerPactBuilder.getConsumerName(), providerName,
          new ProviderState(state, params));
    }

    /**
     * Describe the state the provider needs to be in for the pact test to be verified.
     *
     * @param firstKey Key of first parameter element
     * @param firstValue Value of first parameter element
     * @param paramsKeyValuePair Additional parameters in key-value pairs
     */
    public PactDslWithState given(String state, String firstKey, Object firstValue, Object... paramsKeyValuePair) {

        if (paramsKeyValuePair.length % 2 != 0) {
            throw new IllegalArgumentException("Pair key value should be provided, but there is one key without value.");
        }

        final Map<String, Object> params = new HashMap<>();
        params.put(firstKey, firstValue);

        for (int i = 0; i < paramsKeyValuePair.length; i+=2) {
            params.put(paramsKeyValuePair[i].toString(), paramsKeyValuePair[i+1]);
        }

        return new PactDslWithState(consumerPactBuilder, consumerPactBuilder.getConsumerName(), providerName,
                new ProviderState(state, params));
    }

    /**
     * Description of the request that is expected to be received
     *
     * @param description request description
     */
    public PactDslRequestWithoutPath uponReceiving(String description) {
        return new PactDslWithState(consumerPactBuilder, consumerPactBuilder.getConsumerName(), providerName)
                .uponReceiving(description);
    }

}
