package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.dsl.LambdaDsl
import au.com.dius.pact.consumer.dsl.Matchers
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.SynchronousMessagePactBuilder
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.annotations.Pact
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * This is a test for sync messages. We test that our message consumer can handle the message request
 * configured by the builder and returns a valid response message
 */
@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'MessageProvider', providerType = ProviderType.SYNCH_MESSAGE)
class V4SyncMessageTest {

  // Example Provider client class. This is what sends the request message and then returns the
  // response. We mock it out in the test
  static interface ProviderClient {
    Message process(Message request)
  }

  // Example message handler
  @Canonical
  static class MessageHandler {
    ProviderClient client

    Message process(byte[] data) {
      def json = new JsonSlurper().parse(data) as Map
      def request = new Message(json)
      client.process(request)
    }
  }

  // Example message
  @Canonical
  static class Message {
    String testParam1
    String testParam2
  }

  /**
   * Setup the message interaction. It consists of a request message that is sent to the provider and the
   * response message we expect to receive back.
   */
  @Pact(consumer = 'test_consumer_v4')
  V4Pact createPact(SynchronousMessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody()
    body.stringMatcher('testParam1', '\\w+', 'value1')
    body.stringValue('testParam2', 'value2')

    Map<String, Object> metadata = [destination: Matchers.regexp('\\w+\\d+', 'X001')]

    builder
      .expectsToReceive('a test message')
      .withRequest {
        it.withMetadata(metadata)
        it.withContent(body)
      }
      .withResponse {
        it.withContent(LambdaDsl.newJsonBody {
          it.stringValue('testParam1', 'value3')
          it.stringValue('testParam2', 'value4')
        }.build())
      }
      .toPact()
  }

  /**
   * Test for the first interaction
   */
  @Test
  @PactTestFor(pactMethod = 'createPact')
  void test(V4Interaction.SynchronousMessages message) {
    assert message.request.contents.valueAsString() == '{"testParam1":"value1","testParam2":"value2"}'
    assert message.request.metadata == [destination: 'X001', contentType: 'application/json']
    assert message.request.matchingRules.toMap(PactSpecVersion.V4) == [
      metadata: [destination: [matchers: [[match: 'regex', regex: '\\w+\\d+']], combine: 'AND']],
      body: ['$.testParam1': [matchers: [[match: 'regex', regex: '\\w+']], combine: 'AND']]
    ]

    // We need to process the message here with our actual message handler (it should be the one used to actually
    // process your messages). This example just uses a test class as an example
    ProviderClient mockProvider = [process: { Message request ->
      // validate the request message
      assert request.testParam1 == 'value1'
      assert request.testParam2 == 'value2'

      // generate the response
      def response = new JsonSlurper().parse(message.response.first().contents.value) as Map
      new Message(response)
    }] as ProviderClient
    def processed = new MessageHandler(mockProvider).process(message.request.contents.value)

    assert processed.testParam1 == 'value3'
    assert processed.testParam2 == 'value4'
  }
}
