package au.com.dius.pact.provider.junit.target;

import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.PactSource;

/**
 * Run {@link Interaction} and perform response verification
 *
 * @see HttpTarget out-of-the-box implementation
 */
public interface Target {
    /**
     * Run {@link Interaction} and perform response verification
     * <p>
     * Any exception will be caught by caller and reported as test failure
     * @param consumer consumer name that generated the interaction
     * @param interaction interaction to be tested
     * @param source Source of the Pact interaction
     */
    void testInteraction(String consumer, Interaction interaction, PactSource source);
}
