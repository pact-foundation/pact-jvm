package au.com.dius.pact.model.v3.messaging

import au.com.dius.pact.model.BasePact
import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.InvalidPactException
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.Provider
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j

/**
 * Pact for a sequences of messages
 */
@Slf4j
@ToString(includeSuperProperties = true)
@EqualsAndHashCode(callSuper = true)
@CompileStatic
class MessagePact extends BasePact {
  List<Message> messages = []

  MessagePact(Provider provider, Consumer consumer, List<Message> messages) {
    this(provider, consumer, messages, DEFAULT_METADATA)
  }

  MessagePact(Provider provider, Consumer consumer, List<Message> messages, Map metadata) {
    super(provider, consumer, metadata)
    this.messages = messages
  }

  static MessagePact fromMap(Map map) {
    def consumer = Consumer.fromMap(map.consumer as Map)
    def provider = Provider.fromMap(map.provider as Map)
    def messages = map.messages.collect { Message.fromMap((Map) it) }
    def metadata = map.metadata as Map
    new MessagePact(provider, consumer, messages, metadata)
  }

  @Override
  Map toMap(PactSpecVersion pactSpecVersion) {
    if (pactSpecVersion < PactSpecVersion.V3) {
      throw new InvalidPactException('Message pacts only support version 3+, cannot write pact specification ' +
        "version ${pactSpecVersion}")
    }
    [
      consumer: [name: consumer.name],
      provider: [name: provider.name],
      messages: messages*.toMap(),
      metadata: metadata
    ]
  }

  @Override
  void mergeInteractions(List<Interaction> interactions) {
    messages = (messages + (interactions as List<Message>)).unique { it.description }
    sortInteractions()
  }

  List<Interaction> getInteractions() {
    messages as List<Interaction>
  }

  @Override
  Pact sortInteractions() {
    messages.sort { it.providerState + it.description }
    this
  }

  MessagePact mergePact(Pact other) {
    if (!(other instanceof MessagePact)) {
      throw new InvalidPactException("Unable to merge pact $other as it is not a MessagePact")
    }
    mergeInteractions(other.interactions)
    this
  }

}
