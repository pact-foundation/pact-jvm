package au.com.dius.pact.provider.junit.filter;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.RequestResponseInteraction;

import java.util.Arrays;
import java.util.function.Predicate;

public interface InteractionFilter<I extends Interaction> {

    Predicate<I> buildPredicate(String[] values);

    /**
     * Filter interactions by any of their provider state. If one matches any of the values, the interaction
     * is kept and verified.
     */
    class ByProviderState<I extends Interaction> implements InteractionFilter<I> {

        @Override
        public Predicate<I> buildPredicate(String[] values) {
            return interaction -> Arrays.stream(values).anyMatch(
                value -> interaction.getProviderStates().stream().anyMatch(
                    state -> state .getName() != null && state.getName().matches(value)
                )
            );
        }
    }

    /**
     * Filter interactions by their request path, e.g. with value "^\\/somepath.*".
     */
    class ByRequestPath<I extends Interaction> implements InteractionFilter<I> {

        @Override
        public Predicate<I> buildPredicate(String[] values) {
            return interaction -> {
                if (interaction instanceof RequestResponseInteraction) {
                    return Arrays.stream(values).anyMatch(value ->
                        ((RequestResponseInteraction) interaction).getRequest().getPath().matches(value)
                    );
                } else {
                    return false;
                }
            };
        }
    }
}
