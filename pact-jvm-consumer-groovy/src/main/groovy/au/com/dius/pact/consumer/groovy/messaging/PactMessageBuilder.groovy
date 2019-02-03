package au.com.dius.pact.consumer.groovy.messaging

import au.com.dius.pact.consumer.PactConsumerConfig
import au.com.dius.pact.consumer.groovy.BaseBuilder
import au.com.dius.pact.consumer.groovy.Matcher
import au.com.dius.pact.consumer.groovy.PactBodyBuilder
import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.InvalidPactException
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.Provider
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.v3.messaging.Message
import au.com.dius.pact.model.v3.messaging.MessagePact

/**
 * Pact builder for consumer tests for messaging
 */
class PactMessageBuilder extends BaseBuilder {
  Consumer consumer
  Provider provider
  List<ProviderState> providerStates = []
  List messages = []

  /**
   * Service consumer
   * @param consumer
   */
  PactMessageBuilder serviceConsumer(String consumer) {
    this.consumer = new Consumer(consumer)
    this
  }

  /**
   * Provider that the consumer has a pact with
   * @param provider
   */
  PactMessageBuilder hasPactWith(String provider) {
    this.provider = new Provider(provider)
    this
  }

  /**
   * Provider state required for the message to be produced
   * @param providerState
   */
  PactMessageBuilder given(String providerState) {
    this.providerStates << new ProviderState(providerState)
    this
  }

  /**
   * Description of the message to be received
   * @param description
   */
  PactMessageBuilder expectsToReceive(String description) {
    messages << new Message(description, providerStates)
    this
  }

  /**
   * Metadata attached to the message
   * @param metaData
   */
  PactMessageBuilder withMetaData(Map metaData) {
    if (messages.empty) {
      throw new InvalidPactException('expectsToReceive is required before withMetaData')
    }
    Message message = messages.last()
    message.metaData = metaData.collectEntries {
      if (it.value instanceof Matcher) {
        message.matchingRules.addCategory('metaData').addRule(it.key, it.value.matcher)
        if (it.value.generator) {
          message.generators.addGenerator(au.com.dius.pact.model.generators.Category.METADATA, it.value.generator)
        }
        [it.key, it.value.value]
      } else {
        [it.key, it.value]
      }
    }
    this
  }

  /**
   * Content of the message
   * @param contentType optional content type of the message
   * @deprecated Use version that takes an option map
   */
  @Deprecated
  PactMessageBuilder withContent(String contentType, Closure closure) {
    withContent(contentType: contentType, closure)
  }

  /**
   * Content of the message
   * @param options Options for generating the message content:
   *  - contentType: optional content type of the message
   *  - prettyPrint: if the message content should be pretty printed
   */
  PactMessageBuilder withContent(Map options = [:], Closure closure) {
    if (messages.empty) {
      throw new InvalidPactException('expectsToReceive is required before withContent')
    }
    if (options.contentType) {
      messages.last().metaData.contentType = options.contentType
    }

    def body = new PactBodyBuilder(mimetype: options.contentType, prettyPrintBody: options.prettyPrint)
    closure.delegate = body
    closure.call()
    messages.last().contents = OptionalBody.body(body.body)
    messages.last().matchingRules.addCategory(body.matchers)

    this
  }

  /**
   * Execute the given closure for each defined message
   * @param closure
   */
  void run(Closure closure) {
    def pact = new MessagePact(provider, consumer, messages)
    def results = messages.collect {
      try {
        closure.call(it)
      } catch (ex) {
        ex
      }
    }

    if (results.any { it instanceof Throwable }) {
      throw new MessagePactFailedException(results.findAll { it instanceof Throwable })
    } else {
      pact.write(PactConsumerConfig.pactDirectory, PactSpecVersion.V3)
    }
  }

  @Override
  @SuppressWarnings('UnnecessaryOverridingMethod')
  def call(@DelegatesTo(value = PactMessageBuilder, strategy = Closure.DELEGATE_FIRST) Closure closure) {
	  super.build(closure)
  }

	@Override
  @SuppressWarnings('UnnecessaryOverridingMethod')
	def build(@DelegatesTo(value = PactMessageBuilder, strategy = Closure.DELEGATE_FIRST) Closure closure) {
		super.build(closure)
	}
}
