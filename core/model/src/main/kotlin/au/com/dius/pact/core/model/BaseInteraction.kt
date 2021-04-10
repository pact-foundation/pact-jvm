package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.json.JsonValue

abstract class BaseInteraction(
  override val interactionId: String? = null,
  override val description: String,
  override val providerStates: List<ProviderState> = listOf(),
  override val comments: MutableMap<String, JsonValue> = mutableMapOf()
) : Interaction {
  fun displayState(): String {
    return if (providerStates.isEmpty() || providerStates.size == 1 && providerStates[0].name.isNullOrEmpty()) {
      "None"
    } else {
      providerStates.joinToString(", ") { it.name.toString() }
    }
  }
}
