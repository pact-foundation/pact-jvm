package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.support.json.JsonValue

/**
 * Pact HTTP builder DSL that supports V4 formatted Pact files
 */
open class HttpInteractionBuilder(
  description: String,
  providerStates: MutableList<ProviderState>,
  comments: MutableList<JsonValue.StringValue>
) {
  val interaction = V4Interaction.SynchronousHttp("", description, providerStates)

  init {
    if (comments.isNotEmpty()) {
      interaction.comments["text"] = JsonValue.Array(comments.toMutableList())
    }
  }

  fun build(): V4Interaction {
    return interaction
  }

  /**
   * Build the request part of the interaction using a request builder
   */
  fun withRequest(builderFn: (HttpRequestBuilder) -> HttpRequestBuilder?): HttpInteractionBuilder {
    val builder = HttpRequestBuilder(interaction.request)
    val result = builderFn(builder)
    if (result != null) {
      interaction.request = result.build()
    } else {
      interaction.request = builder.build()
    }
    return this;
  }

  /**
   * Build the response part of the interaction using a response builder
   */
  fun willRespondWith(builderFn: (HttpResponseBuilder) -> HttpResponseBuilder?): HttpInteractionBuilder {
    val builder = HttpResponseBuilder(interaction.response)
    val result = builderFn(builder)
    if (result != null) {
      interaction.response = result.build()
    } else {
      interaction.response = builder.build()
    }
    return this;
  }

  /**
   * Sets the unique key for the interaction. If this is not set, or is empty, a key will be calculated from the
   * contents of the interaction.
   */
  fun key(key: String?): HttpInteractionBuilder {
    interaction.key = key
    return this;
  }

  /**
   * Sets the interaction description
   */
  fun description(description: String): HttpInteractionBuilder {
    interaction.description = description
    return this
  }

  /**
   * Adds a provider state to the interaction.
   */
  @JvmOverloads
  fun state(stateDescription: String, params: Map<String, Any?> = emptyMap()): HttpInteractionBuilder {
    interaction.providerStates.add(ProviderState(stateDescription, params))
    return this
  }

  /**
   * Adds a provider state to the interaction with a parameter.
   */
  fun state(stateDescription: String, paramKey: String, paramValue: Any?): HttpInteractionBuilder {
    interaction.providerStates.add(ProviderState(stateDescription, mapOf(paramKey to paramValue)))
    return this
  }

  /**
   * Adds a provider state to the interaction with parameters a pairs of key/values.
   */
  fun state(stateDescription: String, vararg params: Pair<String, Any?>): HttpInteractionBuilder {
    interaction.providerStates.add(ProviderState(stateDescription, params.toMap()))
    return this
  }

  /**
   * Marks the interaction as pending.
   */
  fun pending(pending: Boolean): HttpInteractionBuilder {
    interaction.pending = pending
    return this
  }

  /**
   * Adds a text comment to the interaction
   */
  fun comment(comment: String): HttpInteractionBuilder {
    if (interaction.comments.containsKey("text")) {
      interaction.comments["text"]!!.add(JsonValue.StringValue(comment))
    } else {
      interaction.comments["text"] = JsonValue.Array(mutableListOf(JsonValue.StringValue(comment)))
    }
    return this
  }
}
