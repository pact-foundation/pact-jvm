package au.com.dius.pact.provider.junitsupport.filter;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.SynchronousRequestResponse;
import au.com.dius.pact.core.model.v4.V4InteractionType;

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
                if (interaction instanceof SynchronousRequestResponse) {
                    return Arrays.stream(values).anyMatch(value ->
                        ((SynchronousRequestResponse) interaction).getRequest().getPath().matches(value)
                    );
                } else {
                    return false;
                }
            };
        }
    }

    /**
     * Filter interactions by type
     */
    class ByInteractionType<I extends Interaction> implements InteractionFilter<I> {
        @Override
        public Predicate<I> buildPredicate(String[] values) {
            return interaction -> Arrays.stream(values).anyMatch(value -> {
                V4InteractionType type = V4InteractionType.Companion.fromString(value).get();
                return type != null && interaction.asV4Interaction().isInteractionType(type);
            });
        }
    }
}
