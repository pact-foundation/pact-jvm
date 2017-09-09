package au.com.dius.pact.model

import java.util.function.Predicate

/**
 * Pact Provider
 */
data class Provider @JvmOverloads constructor (val name: String = "provider") {
  companion object {
    @JvmStatic
    fun fromMap(map: Map<String, Any?>): Provider {
      if (map.containsKey("name") && map["name"] != null) {
        val name = map["name"].toString()
        return Provider(if (name.isEmpty()) "provider" else name)
      }
      return Provider("provider")
    }
  }
}

/**
 * Pact Consumer
 */
data class Consumer @JvmOverloads constructor (val name: String = "consumer") {
  companion object {
    @JvmStatic
    fun fromMap(map: Map<String, Any?>): Consumer {
      if (map.containsKey("name") && map["name"] != null) {
        val name = map["name"].toString()
        return Consumer(if (name.isEmpty()) "consumer" else name)
      }
      return Consumer("consumer")
    }
  }
}

/**
 * Interface to an interaction between a consumer and a provider
 */
interface Interaction {
  /**
   * Interaction description
   */
  val description: String

  /**
   * This just returns the first description from getProviderStates()
   */
  @get:Deprecated("Use getProviderStates()")
  val providerState: String

  /**
   * Returns the provider states for this interaction
   */
  val providerStates: List<ProviderState>

  /**
   * Checks if this interaction conflicts with the other one. Used for merging pact files.
   */
  fun conflictsWith(other: Interaction): Boolean

  /**
   * Converts this interaction to a Map
   */
  fun toMap(pactSpecVersion: PactSpecVersion): Map<*, *>

  fun uniqueKey(): String
}

/**
 * Interface to a pact
 */
interface Pact {
  /**
   * Returns the provider of the service for the pact
   */
  val provider: Provider
  /**
   * Returns the consumer of the service for the pact
   */
  val consumer: Consumer
  /**
   * Returns all the interactions of the pact
   */
  val interactions: List<Interaction>

  /**
   * The source that this pact was loaded from
   */
  val source: PactSource

  /**
   * Returns a pact with the interactions sorted
   */
  fun sortInteractions(): Pact

  /**
   * Returns a Map representation of this pact for the purpose of generating a JSON document.
   */
  fun toMap(pactSpecVersion: PactSpecVersion): Map<String, *>

  /**
   * If this pact is compatible with the other pact. Pacts are compatible if they have the
   * same provider and they are the same type
   */
  fun compatibleTo(other: Pact): Boolean

  /**
   * Merges all the interactions into this Pact
   * @param interactions
   */
  fun mergeInteractions(interactions: List<Interaction>)

  /**
   * Returns a new Pact with all the interactions filtered by the provided predicate
   * @deprecated Wrap the pact in a FilteredPact instead
   */
  @Deprecated("Wrap the pact in a FilteredPact instead")
  fun filterInteractions(predicate: Predicate<Interaction>): Pact
}
