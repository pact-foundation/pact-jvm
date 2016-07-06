package au.com.dius.pact.provider.junit.target;

import au.com.dius.pact.model.RequestResponseInteraction;

/**
 * Run {@link RequestResponseInteraction} and perform response verification
 *
 * @see HttpTarget out-of-the-box implementation
 */
public interface Target {
    /**
     * Run {@link RequestResponseInteraction} and perform response verification
     * <p>
     * Any exception will be caught by caller and reported as test failure
     *
     * @param consumer consumer name that generated the interaction
     * @param interaction interaction to be tested
     */
    void testInteraction(String consumer, RequestResponseInteraction interaction);
}
