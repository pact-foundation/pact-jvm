package au.com.dius.pact.model

/**
 * Interface to a pact
 */
interface Pact {
  Provider getProvider()
  Consumer getConsumer()
  List<Interaction> getInteractions()
  Pact sortInteractions()
}
