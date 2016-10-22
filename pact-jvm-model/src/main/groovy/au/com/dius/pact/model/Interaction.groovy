package au.com.dius.pact.model

/**
 * Interface to an interaction between a consumer and a provider
 */
interface Interaction {
  String getDescription()
  String getProviderState()
  boolean conflictsWith(Interaction other)
  String uniqueKey()
}
