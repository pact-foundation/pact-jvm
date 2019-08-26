package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import com.github.salomonbrys.kotson.obj
import com.google.gson.JsonElement

/**
 * Pact Provider
 */
data class Provider @JvmOverloads constructor (val name: String = "provider") {
  companion object {
    @JvmStatic
    fun fromJson(json: JsonElement): Provider {
      if (json.isJsonObject && json.obj.has("name") && json.obj["name"].isJsonPrimitive) {
        val name = Json.toString(json.obj["name"])
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
    fun fromJson(json: JsonElement): Consumer {
      if (json.isJsonObject && json.obj.has("name") && json.obj["name"].isJsonPrimitive) {
        val name = Json.toString(json.obj["name"])
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

  /**
   * Generates a unique key for this interaction
   */
  fun uniqueKey(): String

  /**
   * Interaction ID. Will only be populated from pacts loaded from a Pact Broker
   */
  val interactionId: String?
}

/**
 * Interface to a pact
 */
interface Pact<I : Interaction> {
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
  val interactions: List<I>

  /**
   * The source that this pact was loaded from
   */
  val source: PactSource

  /**
   * Returns a pact with the interactions sorted
   */
  fun sortInteractions(): Pact<I>

  /**
   * Returns a Map representation of this pact for the purpose of generating a JSON document.
   */
  fun toMap(pactSpecVersion: PactSpecVersion): Map<String, *>

  /**
   * If this pact is compatible with the other pact. Pacts are compatible if they have the
   * same provider and they are the same type
   */
  fun compatibleTo(other: Pact<*>): Boolean

  /**
   * Merges all the interactions into this Pact
   * @param interactions
   */
  fun mergeInteractions(interactions: List<*>)
}
