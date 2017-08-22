package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.model.*;
import au.com.dius.pact.model.PactSource;

import java.io.IOException;
import java.util.List;

/**
 * Encapsulate logic for loading pacts
 */
public interface PactLoader {
  /**
   * Load pacts from appropriate source
   *
   * @param providerName name of provider for which pacts will be loaded
   * @return list of pacts
   */
  List<Pact> load(String providerName) throws IOException;

  /**
   * Returns the source object that the pacts where loaded from
   */
  PactSource getPactSource();
}
