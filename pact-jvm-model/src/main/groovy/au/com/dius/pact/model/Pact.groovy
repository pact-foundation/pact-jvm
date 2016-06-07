package au.com.dius.pact.model

/**
 * Interface to a pact
 */
interface Pact {
  /**
   * Returns the provider of the service for the pact
   */
  Provider getProvider()
  /**
   * Returns the consumer of the service for the pact
   */
  Consumer getConsumer()
  /**
   * Returns all the interactions of the pact
   */
  List<Interaction> getInteractions()
  /**
   * Returns a pact with the interactions sorted
   */
  Pact sortInteractions()
  /**
   * Returns a Map representation of this pact for the purpose of generating a JSON document.
   */
  Map toMap(PactSpecVersion pactSpecVersion)
}
