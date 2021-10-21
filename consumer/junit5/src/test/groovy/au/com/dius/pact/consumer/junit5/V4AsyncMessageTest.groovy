package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MessagePactBuilder
import au.com.dius.pact.consumer.dsl.Matchers
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.annotations.Pact
import groovy.json.JsonParser
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * This is a test for async messages. We test that our message consumer can handle the messages
 * configured by the builder
 */
@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'MessageProvider', providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
class V4AsyncMessageTest {

  // Example message handler
  static class MessageHandler {
    static ProcessedMessage process(byte[] data) {
      def json = new JsonSlurper().parse(data) as Map
      new ProcessedMessage(json)
    }
  }

  // Example processed message
  @Canonical
  static class ProcessedMessage {
    String testParam1
    String testParam2
  }

  /**
   * Set the first message interaction (with matching rules)
   */
  @Pact(consumer = 'test_consumer_v4')
  V4Pact createPact(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody()
    body.stringMatcher('testParam1', '\\w+', 'value1')
    body.stringValue('testParam2', 'value2')

    Map<String, Object> metadata = [destination: Matchers.regexp('\\w+\\d+', 'X001')]

    builder.given('SomeProviderState')
      .expectsToReceive('a test message')
      .withMetadata(metadata)
      .withContent(body)
      .toPact()
  }

  /**
   * Setup the second message interaction (with plain data)
   */
  @Pact(provider = 'MessageProvider', consumer = 'test_consumer_v4')
  V4Pact createPact2(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody()
    body.stringValue('testParam1', 'value3')
    body.stringValue('testParam2', 'value4')

    Map<String, String> metadata = ['Content-Type': 'application/json']

    builder.given('SomeProviderState2')
      .expectsToReceive('a test message')
      .withMetadata(metadata)
      .withContent(body)
      .toPact()
  }

  /**
   * Test for the first interaction
   */
  @Test
  @PactTestFor(pactMethod = 'createPact')
  void test(V4Interaction.AsynchronousMessage message) {
    assert message.contents.contents.valueAsString() == '{"testParam1":"value1","testParam2":"value2"}'
    assert message.contents.metadata == [destination: 'X001', contentType: 'application/json']
    assert message.contents.matchingRules.toMap(PactSpecVersion.V4) == [
      metadata: [destination: [matchers: [[match: 'regex', regex: '\\w+\\d+']], combine: 'AND']],
      body: ['$.testParam1': [matchers: [[match: 'regex', regex: '\\w+']], combine: 'AND']]
    ]
    
    // We need to process the message here with our actual message handler (it should be the one used to actually
    // process your messages). This example just uses a test class as an example
    def processed = new MessageHandler().process(message.contents.contents.value)
    assert processed.testParam1 == 'value1'
    assert processed.testParam2 == 'value2'
  }

  /**
   * Test for the second interaction. Here we inject the Pact instead of just the message. We can also inject a list
   * of messages if there are more than one message setup in the interaction
   */
  @Test
  @PactTestFor(pactMethod = 'createPact2')
  void test2(V4Pact pact) {
    assert pact.interactions.size() == 1
    assert pact.interactions[0].contents.contents.valueAsString() == '{"testParam1":"value3","testParam2":"value4"}'

    // We need to process the message here with our actual message handler (it should be the one used to actually
    // process your messages). This example just uses a test class as an example
    def processed = new MessageHandler().process(pact.interactions.first().asAsynchronousMessage().contents.contents.value)
    assert processed.testParam1 == 'value3'
    assert processed.testParam2 == 'value4'
  }
}
