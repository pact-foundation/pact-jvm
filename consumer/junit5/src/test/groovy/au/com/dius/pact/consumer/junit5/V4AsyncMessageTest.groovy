package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MessagePactBuilder
import au.com.dius.pact.consumer.dsl.Matchers
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.annotations.Pact
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'MessageProvider', providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
class V4AsyncMessageTest {

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

  @Test
  @PactTestFor(pactMethod = 'createPact')
  void test(List<V4Interaction.AsynchronousMessage> messages) {
    assert messages[0].contents.valueAsString() == '{"testParam1":"value1","testParam2":"value2"}'
    assert messages[0].metadata == [destination: 'X001', contentType: 'application/json']
    assert messages[0].matchingRules.toMap(PactSpecVersion.V4) == [
      metadata: [destination: [matchers: [[match: 'regex', regex: '\\w+\\d+']], combine: 'AND']],
      content: ['$.testParam1': [matchers: [[match: 'regex', regex: '\\w+']], combine: 'AND']]
    ]
  }

  @Test
  @PactTestFor(pactMethod = 'createPact2')
  void test2(V4Pact pact) {
    assert pact.interactions[0].contents.valueAsString() == '{"testParam1":"value3","testParam2":"value4"}'
  }
}
