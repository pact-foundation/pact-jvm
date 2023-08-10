package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.support.json.JsonValue

/**
 * Pact Message builder DSL that supports V4 formatted Pact files
 */
open class MessageInteractionBuilder(
  description: String,
  providerStates: MutableList<ProviderState>,
  comments: MutableList<JsonValue.StringValue>
) {
  val interaction = V4Interaction.AsynchronousMessage(description, providerStates)

  init {
    if (comments.isNotEmpty()) {
      interaction.comments["text"] = JsonValue.Array(comments.toMutableList())
    }
  }

  fun build(): V4Interaction {
    return interaction
  }
}
