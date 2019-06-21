package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.dsl.Matcher
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.InvalidPactException
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessagePact
import org.apache.http.entity.ContentType

/**
 * PACT DSL builder for v3 specification
 */
class MessagePactBuilder(
  /**
   * The consumer for the pact.
   */
  private var consumer: Consumer,

  /**
   * The provider for the pact.
   */
  private var provider: Provider = Provider(),

  /**
   * Provider states
   */
  private var providerStates: MutableList<ProviderState> = mutableListOf(),

  /**
   * Messages for the pact
   */
  private var messages: MutableList<Message> = mutableListOf()
) {

  /**
   * Name the provider that the consumer has a pact with.
   *
   * @param provider provider name
   * @return this builder.
   */
  fun hasPactWith(provider: String): MessagePactBuilder {
    this.provider = Provider(provider)
    return this
  }

  /**
   * Sets the provider state.
   *
   * @param providerState state of the provider
   * @return this builder.
   */
  fun given(providerState: String): MessagePactBuilder {
    this.providerStates.add(ProviderState(providerState))
    return this
  }

  /**
   * Adds a message expectation in the pact.
   *
   * @param description message description.
   */
  fun expectsToReceive(description: String): MessagePactBuilder {
    messages.add(Message(description, providerStates))
    return this
  }

  /**
   *  Adds the expected metadata to the message
   */
  fun withMetadata(metadata: Map<String, Any>): MessagePactBuilder {
    if (messages.isEmpty()) {
      throw InvalidPactException("expectsToReceive is required before withMetaData")
    }

    val message = messages.last()
    message.metaData = metadata.mapValues { (key, value) ->
      if (value is Matcher) {
        message.matchingRules.addCategory("metadata").addRule(key, value.matcher!!)
        if (value.generator != null) {
          message.generators.addGenerator(category = au.com.dius.pact.core.model.generators.Category.METADATA,
            generator = value.generator!!)
        }
        value.value.toString()
      } else {
        value.toString()
      }
    }
    return this
  }

  /**
   * Adds the JSON body as the message content
   */
  fun withContent(body: DslPart): MessagePactBuilder {
    if (messages.isEmpty()) {
      throw InvalidPactException("expectsToReceive is required before withMetaData")
    }

    val message = messages.last()
    val metadata = message.metaData.toMutableMap()
    val contentType = metadata.entries.find {
      it.key.toLowerCase() == "contenttype" || it.key.toLowerCase() == "content-type"
    }
    if (contentType == null) {
      metadata["contentType"] = ContentType.APPLICATION_JSON.toString()
    } else {
      metadata.remove(contentType.key)
      metadata["contentType"] = contentType.value
    }

    val parent = body.close()
    message.contents = OptionalBody.body(parent.toString().toByteArray(),
      au.com.dius.pact.core.model.ContentType(metadata["contentType"].toString()))
    message.metaData = metadata
    message.matchingRules.addCategory(parent.matchers)

    return this
  }

  /**
   * Convert this builder into a Pact
   */
  fun toPact() = MessagePact(provider, consumer, messages)

  companion object {
    /**
     * Name the consumer of the pact
     *
     * @param consumer Consumer name
     */
    @JvmStatic
    fun consumer(consumer: String) = MessagePactBuilder(Consumer(consumer))
  }
}
