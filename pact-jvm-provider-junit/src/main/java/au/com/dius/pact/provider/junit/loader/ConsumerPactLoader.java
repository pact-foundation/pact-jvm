package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.model.Pact;

import java.io.IOException;
import java.util.List;

/**
 * Encapsulate logic for loading pacts
 */
public interface ConsumerPactLoader {
    /**
     * Load pacts from appropriate source
     *
     * @param providerName name of provider for which pacts will be loaded
     * @param consumerName name of consumer for which pacts will be loaded
     * @return list of pacts
     */
    List<Pact> load(String providerName, String consumerName) throws IOException;
}
