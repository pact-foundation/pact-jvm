package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.model.ProviderState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PactDslWithState {
    private final ConsumerPactBuilder consumerPactBuilder;
    List<ProviderState> state;
    String consumerName;
    String providerName;

    PactDslWithState(ConsumerPactBuilder consumerPactBuilder, String consumerName, String providerName, ProviderState state) {
        this(consumerPactBuilder, consumerName, providerName);
        this.state.add(state);
    }

    PactDslWithState(ConsumerPactBuilder consumerPactBuilder, String consumerName, String providerName) {
      this.consumerPactBuilder = consumerPactBuilder;
      this.consumerName = consumerName;
      this.providerName = providerName;
      this.state = new ArrayList<>();
    }

    /**
     * Description of the request that is expected to be received
     *
     * @param description request description
     */
    public PactDslRequestWithoutPath uponReceiving(String description) {
        return new PactDslRequestWithoutPath(consumerPactBuilder, this, description);
    }

    /**
     * Adds another provider state to this interaction
     * @param stateDesc Description of the state
     */
    public PactDslWithState given(String stateDesc) {
      state.add(new ProviderState(stateDesc));
      return this;
    }

    /**
     * Adds another provider state to this interaction
     * @param stateDesc Description of the state
     * @param params State data parameters
     */
    public PactDslWithState given(String stateDesc, Map<String, String> params) {
        state.add(new ProviderState(stateDesc, params));
        return this;
    }
}
