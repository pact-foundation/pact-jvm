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

  /**
   * Sets the unique key for the interaction. If this is not set, or is empty, a key will be calculated from the
   * contents of the interaction.
   */
  fun key(key: String?): MessageInteractionBuilder {
    interaction.key = key
    return this;
  }

  /**
   * Sets the interaction description
   */
  fun description(description: String): MessageInteractionBuilder {
    interaction.description = description
    return this
  }

  /**
   * Adds a provider state to the interaction.
   */
  @JvmOverloads
  fun state(stateDescription: String, params: Map<String, Any?> = emptyMap()): MessageInteractionBuilder {
    interaction.providerStates.add(ProviderState(stateDescription, params))
    return this
  }

  /**
   * Adds a provider state to the interaction with a parameter.
   */
  fun state(stateDescription: String, paramKey: String, paramValue: Any?): MessageInteractionBuilder {
    interaction.providerStates.add(ProviderState(stateDescription, mapOf(paramKey to paramValue)))
    return this
  }

  /**
   * Adds a provider state to the interaction with parameters a pairs of key/values.
   */
  fun state(stateDescription: String, vararg params: Pair<String, Any?>): MessageInteractionBuilder {
    interaction.providerStates.add(ProviderState(stateDescription, params.toMap()))
    return this
  }

  /**
   * Marks the interaction as pending.
   */
  fun pending(pending: Boolean): MessageInteractionBuilder {
    interaction.pending = pending
    return this
  }

  /**
   * Adds a text comment to the interaction
   */
  fun comment(comment: String): MessageInteractionBuilder {
    interaction.addTextComment(comment)
    return this
  }

  /**
   * Build the contents of the interaction using a contents builder
   */
  fun withContents(builderFn: (MessageContentsBuilder) -> MessageContentsBuilder?): MessageInteractionBuilder {
    val builder = MessageContentsBuilder(interaction.contents)
    val result = builderFn(builder)
    if (result != null) {
      interaction.contents = result.contents
    } else {
      interaction.contents = builder.contents
    }
    return this;
  }

  fun build(): V4Interaction {
    return interaction
  }
}
