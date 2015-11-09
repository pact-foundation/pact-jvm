package au.com.dius.pact.provider.junit.target;

import au.com.dius.pact.model.Interaction;

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
     *
     * @param interaction interaction to be tested
     */
    void testInteraction(Interaction interaction);
}
