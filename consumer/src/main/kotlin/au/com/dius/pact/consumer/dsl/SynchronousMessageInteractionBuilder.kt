package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.core.support.json.JsonValue

/**
 * Pact Message builder DSL that supports V4 formatted Pact files
 */
open class SynchronousMessageInteractionBuilder(
  description: String,
  providerStates: MutableList<ProviderState>,
  comments: MutableList<JsonValue.StringValue>
) {
  val interaction = V4Interaction.SynchronousMessages(description, providerStates)

  init {
    if (comments.isNotEmpty()) {
      interaction.comments["text"] = JsonValue.Array(comments.toMutableList())
    }
  }

  /**
   * Sets the unique key for the interaction. If this is not set, or is empty, a key will be calculated from the
   * contents of the interaction.
   */
  fun key(key: String?): SynchronousMessageInteractionBuilder {
    interaction.key = key
    return this;
  }

  /**
   * Sets the interaction description
   */
  fun description(description: String): SynchronousMessageInteractionBuilder {
    interaction.description = description
    return this
  }

  /**
   * Adds a provider state to the interaction.
   */
  @JvmOverloads
  fun state(stateDescription: String, params: Map<String, Any?> = emptyMap()): SynchronousMessageInteractionBuilder {
    interaction.providerStates.add(ProviderState(stateDescription, params))
    return this
  }

  /**
   * Adds a provider state to the interaction with a parameter.
   */
  fun state(stateDescription: String, paramKey: String, paramValue: Any?): SynchronousMessageInteractionBuilder {
    interaction.providerStates.add(ProviderState(stateDescription, mapOf(paramKey to paramValue)))
    return this
  }

  /**
   * Adds a provider state to the interaction with parameters a pairs of key/values.
   */
  fun state(stateDescription: String, vararg params: Pair<String, Any?>): SynchronousMessageInteractionBuilder {
    interaction.providerStates.add(ProviderState(stateDescription, params.toMap()))
    return this
  }

  /**
   * Marks the interaction as pending.
   */
  fun pending(pending: Boolean): SynchronousMessageInteractionBuilder {
    interaction.pending = pending
    return this
  }

  /**
   * Adds a text comment to the interaction
   */
  fun comment(comment: String): SynchronousMessageInteractionBuilder {
    interaction.addTextComment(comment)
    return this
  }

  /**
   * Build the request part of the interaction using a contents builder
   */
  fun withRequest(
    builderFn: (MessageContentsBuilder) -> MessageContentsBuilder?
  ): SynchronousMessageInteractionBuilder {
    val builder = MessageContentsBuilder(interaction.request)
    val result = builderFn(builder)
    if (result != null) {
      interaction.request = result.contents
    } else {
      interaction.request = builder.contents
    }
    return this;
  }

  /**
   * Build the response part of the interaction using a response builder. This can be called multiple times to add
   * additional response messages.
   */
  fun willRespondWith(
    builderFn: (MessageContentsBuilder) -> MessageContentsBuilder?
  ): SynchronousMessageInteractionBuilder {
    val builder = MessageContentsBuilder(MessageContents())
    val result = builderFn(builder)
    if (result != null) {
      interaction.response.add(result.contents)
    } else {
      interaction.response.add(builder.contents)
    }
    return this;
  }

  fun build(): V4Interaction {
    return interaction
  }
}
