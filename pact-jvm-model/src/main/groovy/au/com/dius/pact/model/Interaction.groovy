package au.com.dius.pact.model

/**
 * Interface to an interaction between a consumer and a provider
 */
interface Interaction {
  /**
   * Interaction description
   */
  String getDescription()

  /**
   * This just returns the first description from getProviderStates()
   * @deprecated Use getProviderStates()
   */
  @Deprecated
  String getProviderState()

  /**
   * Returns the provider states for this interaction
   */
  List<ProviderState> getProviderStates()

  /**
   * Checks if this interaction conflicts with the other one. Used for merging pact files.
   */
  boolean conflictsWith(Interaction other)

  /**
   * Converts this interaction to a Map
   */
  Map toMap(PactSpecVersion pactSpecVersion)
  String uniqueKey()
}
