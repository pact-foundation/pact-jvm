package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.core.model.PactSource;
import au.com.dius.pact.core.support.expressions.ValueResolver;

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

  /**
   * Sets the value resolver to use to resolve property expressions. By default a system property resolver will be used.
   *
   * @param valueResolver Value Resolver
   */
  default void setValueResolver(ValueResolver valueResolver) { }

  /**
   * Returns a description of this pact loader
   */
  default String description() { return this.getClass().getSimpleName(); };

  /**
   * Enables pending pact feature
   */
  default void enablePendingPacts(boolean flag) { };
}
