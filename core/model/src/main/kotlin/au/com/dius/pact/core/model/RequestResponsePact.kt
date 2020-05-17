package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import com.github.salomonbrys.kotson.jsonObject

/**
 * Pact between a consumer and a provider
 */
class RequestResponsePact @JvmOverloads constructor(
  override val provider: Provider,
  override val consumer: Consumer,
  interactions: List<RequestResponseInteraction> = listOf(),
  override val metadata: Map<String, Any?> = DEFAULT_METADATA,
  override val source: PactSource = UnknownPactSource
) : BasePact<RequestResponseInteraction>(consumer, provider, metadata, source) {

  override var interactions = interactions.toMutableList()

  override fun sortInteractions(): Pact<RequestResponseInteraction> {
    interactions.sortBy { interaction -> interaction.providerStates.joinToString { it.name.toString() } +
      interaction.description }
    return this
  }

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> = mapOf(
    "provider" to objectToMap(provider),
    "consumer" to objectToMap(consumer),
    "interactions" to interactions.map { it.toMap(pactSpecVersion) },
    "metadata" to metaData(jsonObject(metadata.entries.map { it.key to Json.toJson(it.value) }), pactSpecVersion)
  )

  override fun mergeInteractions(interactions: List<*>) {
    interactions as List<RequestResponseInteraction>
    this.interactions = (this.interactions + interactions).distinctBy { it.uniqueKey() }.toMutableList()
    sortInteractions()
  }

  fun interactionFor(description: String, providerState: String): RequestResponseInteraction? {
    return interactions.find { i ->
      i.description == description && i.providerStates.any { it.name == providerState }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as RequestResponsePact

    if (provider != other.provider) return false
    if (consumer != other.consumer) return false
    if (interactions != other.interactions) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + provider.hashCode()
    result = 31 * result + consumer.hashCode()
    result = 31 * result + interactions.hashCode()
    return result
  }
}
