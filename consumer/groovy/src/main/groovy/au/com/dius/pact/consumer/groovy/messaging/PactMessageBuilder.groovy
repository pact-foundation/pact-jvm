package au.com.dius.pact.consumer.groovy.messaging

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
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.messaging.MessageInteraction
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.core.support.BuiltToolConfig
import au.com.dius.pact.core.support.MetricEvent
import au.com.dius.pact.core.support.Metrics

/**
 * Pact builder for consumer tests for asynchronous messaging
 */
class PactMessageBuilder extends GroovyBuilder {
  Consumer consumer
  Provider provider
  List<ProviderState> providerStates = []
  List<V4Interaction.AsynchronousMessage> messages = []

  PactMessageBuilder(PactSpecVersion pactVersion) {
    super(pactVersion ?: PactSpecVersion.V4)
  }

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
   * Enable the plugin
   * @param name Plugin Name
   * @param version Plugin Version
   */
  @Override
  PactMessageBuilder usingPlugin(String name, String version) {
    super.usingPlugin(name, version) as PactMessageBuilder
  }

  /**
   * Enable the plugin
   * @param name Plugin Name
   * @return
   */
  @Override
  PactMessageBuilder usingPlugin(String name) {
    super.usingPlugin(name) as PactMessageBuilder
  }

  /**
   * Description of the message to be received
   * @param description
   */
  PactMessageBuilder expectsToReceive(String description, String key = '') {
    def message = new V4Interaction.AsynchronousMessage(key, description, new MessageContents(), null, providerStates)
    messages << message
    this
  }

  /**
   * Metadata attached to the message
   * @param metaData
   */
  PactMessageBuilder withMetaData(Map metadata) {
    this.withMetadata(metadata)
  }

  /**
   * Metadata attached to the message
   * @param metaData
   */
  @SuppressWarnings('ConfusingMethodName')
  PactMessageBuilder withMetadata(Map metadata) {
    if (messages.empty) {
      throw new InvalidPactException('expectsToReceive is required before withMetaData')
    }
    V4Interaction.AsynchronousMessage message = messages.last()
    message.withMetadata(metadata.collectEntries {
      if (it.value instanceof Matcher) {
        message.contents.matchingRules.addCategory('metadata').addRule(it.key, it.value.matcher)
        if (it.value.generator) {
          message.contents.generators.addGenerator(
            au.com.dius.pact.model.generators.Category.METADATA, it.key, it.value.generator
          )
        }
        [it.key, it.value.value]
      } else {
        [it.key, it.value]
      }
    })
    this
  }

  /**
   * Content of the message
   * @param options Options for generating the message content:
   *  - contentType: optional content type of the message
   *  - prettyPrint: if the message content should be pretty printed
   */
  PactMessageBuilder withContent(Map options = [:], def value) {
    if (messages.empty) {
      throw new InvalidPactException('expectsToReceive is required before withContent')
    }

    V4Interaction.AsynchronousMessage message = messages.last()
    def contentType = ContentType.JSON.contentType

    def messageContents = message.contents
    if (options.contentType) {
      contentType = options.contentType
      messageContents.metadata.contentType = options.contentType
    } else if (messageContents.metadata.contentType) {
      contentType = messageContents.metadata.contentType
    }

    if (value instanceof Closure) {
      Closure closure = value as Closure
      def body = new PactBodyBuilder(mimetype: contentType, prettyPrintBody: options.prettyPrint)
      closure.delegate = body
      closure.call()
      messageContents.matchingRules.addCategory(body.matchers)
      message.contents = new MessageContents(OptionalBody.body(body.body.bytes, new ContentType(contentType)),
        messageContents.metadata, messageContents.matchingRules, messageContents.generators, messageContents.partName)
    } else {
      messages.last().contents =  new MessageContents(
        OptionalBody.body(value.toString().bytes, new ContentType(contentType)),
        messageContents.metadata, messageContents.matchingRules, messageContents.generators, messageContents.partName
      )
    }

    this
  }

  /**
   * Execute the given closure for each defined message
   * @param closure
   */
  void run(Closure closure) {
    def pact = new V4Pact(consumer, provider, messages)
    def results = messages.collect {
      try {
        closure.call(it as MessageInteraction)
      } catch (ex) {
        ex
      }
    }

    Metrics.INSTANCE.sendMetrics(new MetricEvent.ConsumerTestRun(messages.size(), 'groovy'))

    if (results.any { it instanceof Throwable }) {
      throw new MessagePactFailedException(results.findAll { it instanceof Throwable })
    } else {
      if (pactVersion >= PactSpecVersion.V4) {
        pact.write(BuiltToolConfig.INSTANCE.pactDirectory, pactVersion)
      } else {
        pact.asMessagePact()
          .expect { "Error converting Pact to V3 format - $it" }
          .write(BuiltToolConfig.INSTANCE.pactDirectory, pactVersion)
      }
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
