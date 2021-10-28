package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.MessageContentsBuilder
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.InvalidPactException
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.v4.MessageContents

/**
 * PACT DSL builder for v4 specification synchronous request/response messages
 */
class SynchronousMessagePactBuilder @JvmOverloads constructor(
  /**
   * The consumer for the pact.
   */
  private var consumer: Consumer = Consumer(),

  /**
   * The provider for the pact.
   */
  private var provider: Provider = Provider(),

  /**
   * Provider states
   */
  private var providerStates: MutableList<ProviderState> = mutableListOf(),

  /**
   * Interactions for the pact
   */
  private var messages: MutableList<V4Interaction.SynchronousMessages> = mutableListOf(),

  /**
   * Specification Version
   */
  private var specVersion: PactSpecVersion = PactSpecVersion.V4
) {
  constructor(specVersion: PactSpecVersion) :
    this(Consumer(), Provider(), mutableListOf(), mutableListOf(), specVersion)

  init {
    if (specVersion < PactSpecVersion.V4) {
      throw IllegalArgumentException("SynchronousMessagePactBuilder requires at least V4 Pact specification")
    }
  }

  /**
   * Name the consumer of the pact
   *
   * @param consumer Consumer name
   */
  fun consumer(consumer: String): SynchronousMessagePactBuilder {
    this.consumer = Consumer(consumer)
    return this
  }

  /**
   * Name the provider that the consumer has a pact with.
   *
   * @param provider provider name
   * @return this builder.
   */
  fun hasPactWith(provider: String): SynchronousMessagePactBuilder {
    this.provider = Provider(provider)
    return this
  }

  /**
   * Sets the provider state.
   *
   * @param providerState description of the provider state
   * @return this builder.
   */
  fun given(providerState: String): SynchronousMessagePactBuilder {
    this.providerStates.add(ProviderState(providerState))
    return this
  }

  /**
   * Sets the provider state.
   *
   * @param providerState description of the provider state
   * @param params key/value pairs to describe state
   * @return this builder.
   */
  fun given(providerState: String, params: Map<String, Any>): SynchronousMessagePactBuilder {
    this.providerStates.add(ProviderState(providerState, params))
    return this
  }

  /**
   * Sets the provider state.
   *
   * @param providerState state of the provider
   * @return this builder.
   */
  fun given(providerState: ProviderState): SynchronousMessagePactBuilder {
    this.providerStates.add(providerState)
    return this
  }

  /**
   * Adds a message expectation to the pact.
   *
   * @param description message description.
   */
  fun expectsToReceive(description: String): SynchronousMessagePactBuilder {
    messages.add(V4Interaction.SynchronousMessages("", description, providerStates = providerStates))
    return this
  }

  /**
   *  Adds the expected request message to the interaction
   */
  fun withRequest(callback: java.util.function.Consumer<MessageContentsBuilder>): SynchronousMessagePactBuilder {
    if (messages.isEmpty()) {
      throw InvalidPactException("expectsToReceive is required before withRequest")
    }

    val message = messages.last()
    val builder = MessageContentsBuilder(message.request)
    callback.accept(builder)
    message.request = builder.contents

    return this
  }

  /**
   *  Adds the expected response message to the interaction. Calling this multiple times will add a new response message
   *  for each call.
   */
  fun withResponse(callback: java.util.function.Consumer<MessageContentsBuilder>): SynchronousMessagePactBuilder {
    if (messages.isEmpty()) {
      throw InvalidPactException("expectsToReceive is required before withResponse")
    }

    val message = messages.last()
    val builder = MessageContentsBuilder(MessageContents())
    callback.accept(builder)
    message.response.add(builder.contents)

    return this
  }

  /**
   * Terminates the DSL and builds a pact to represent the interactions
   */
  fun <P : Pact?> toPact(pactClass: Class<P>): P {
    return when {
      pactClass.isAssignableFrom(V4Pact::class.java) -> {
        V4Pact(consumer, provider, messages.toMutableList()) as P
      }
      else -> {
        throw IllegalArgumentException(pactClass.simpleName + " is not a valid V4 Pact class")
      }
    }
  }

  /**
   * Convert this builder into a Pact
   */
  fun toPact(): V4Pact {
    return if (specVersion == PactSpecVersion.V4) {
      V4Pact(consumer, provider, messages.toMutableList())
    } else {
      throw IllegalArgumentException("SynchronousMessagePactBuilder requires at least V4 Pact specification")
    }
  }
}
