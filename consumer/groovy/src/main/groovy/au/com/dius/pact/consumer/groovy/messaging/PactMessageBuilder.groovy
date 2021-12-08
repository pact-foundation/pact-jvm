package au.com.dius.pact.consumer.groovy.messaging

import au.com.dius.pact.consumer.PactConsumerConfig
import au.com.dius.pact.consumer.groovy.GroovyBuilder
import au.com.dius.pact.consumer.groovy.Matcher
import au.com.dius.pact.consumer.groovy.PactBodyBuilder
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.InvalidPactException
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.support.MetricEvent
import au.com.dius.pact.core.support.Metrics

/**
 * Pact builder for consumer tests for messaging
 */
class PactMessageBuilder extends GroovyBuilder {
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
  PactMessageBuilder withMetaData(Map metadata) {
    if (messages.empty) {
      throw new InvalidPactException('expectsToReceive is required before withMetaData')
    }
    Message message = messages.last()
    message.metaData = metadata.collectEntries {
      if (it.value instanceof Matcher) {
        message.matchingRules.addCategory('metadata').addRule(it.key, it.value.matcher)
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
   * @param options Options for generating the message content:
   *  - contentType: optional content type of the message
   *  - prettyPrint: if the message content should be pretty printed
   */
  PactMessageBuilder withContent(Map options = [:], Closure closure) {
    if (messages.empty) {
      throw new InvalidPactException('expectsToReceive is required before withContent')
    }

    def contentType = ContentType.JSON.contentType
    if (options.contentType) {
      contentType = options.contentType
      messages.last().metaData.contentType = options.contentType
    } else if (messages.last().metaData.contentType) {
      contentType = messages.last().metaData.contentType
    }

    def body = new PactBodyBuilder(mimetype: contentType, prettyPrintBody: options.prettyPrint)
    closure.delegate = body
    closure.call()
    messages.last().contents = OptionalBody.body(body.body.bytes, new ContentType(contentType))
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

    Metrics.INSTANCE.sendMetrics(new MetricEvent.ConsumerTestRun(messages.size(), 'groovy'))

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
